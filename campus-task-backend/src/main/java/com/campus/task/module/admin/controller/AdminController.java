package com.campus.task.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.enums.TaskStatus;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.R;
import com.campus.task.module.payment.entity.FeeConfig;
import com.campus.task.module.payment.mapper.FeeConfigMapper;
import com.campus.task.module.payment.mapper.SettlementMapper;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.entity.TaskCategory;
import com.campus.task.module.task.mapper.TaskCategoryMapper;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.task.vo.TaskCategoryVO;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员接口（需要ADMIN角色）
 */
@Tag(name = "管理员模块")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final FeeConfigMapper feeConfigMapper;
    private final SettlementMapper settlementMapper;
    private final StringRedisTemplate redisTemplate;
    private final TaskCategoryMapper taskCategoryMapper;

    // ==================== 用户管理 ====================

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public R<Page<User>> users(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getStudentId, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        // 返回前清除密码字段
        Page<User> p = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        p.getRecords().forEach(u -> { u.setPassword(null); u.setSalt(null); });
        return R.ok(p);
    }

    @Operation(summary = "禁用/启用用户")
    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id,
                                    @RequestBody Map<String, Integer> body) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(body.get("status"));
        userMapper.updateById(user);
        // 如果禁用，强制登出
        if (body.get("status") == 1) redisTemplate.delete("token:" + id);
        return R.ok();
    }

    // ==================== 任务管理 ====================

    @Operation(summary = "待审核任务列表")
    @GetMapping("/tasks/pending")
    public R<Page<Task>> pendingTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Task> p = taskMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, TaskStatus.PENDING_AUDIT.getCode())
                        .orderByAsc(Task::getCreatedAt));
        return R.ok(p);
    }

    @Operation(summary = "审核任务")
    @PutMapping("/tasks/{id}/audit")
    public R<Void> auditTask(@PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getStatus() != TaskStatus.PENDING_AUDIT.getCode())
            throw new BusinessException("任务不在待审核状态");
        Boolean pass = (Boolean) body.get("pass");
        if (Boolean.TRUE.equals(pass)) {
            task.setStatus(TaskStatus.PENDING_PAYMENT.getCode());
        } else {
            task.setStatus(TaskStatus.CANCELLED.getCode());
            task.setRejectReason(body.getOrDefault("rejectReason", "审核未通过").toString());
        }
        taskMapper.updateById(task);
        return R.ok();
    }

    @Operation(summary = "删除违规任务")
    @DeleteMapping("/tasks/{id}")
    public R<Void> deleteTask(@PathVariable Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException("任务不存在");
        task.setStatus(TaskStatus.CANCELLED.getCode());
        taskMapper.updateById(task);
        return R.ok();
    }

    // ==================== 手续费配置 ====================

    @Operation(summary = "获取手续费配置")
    @GetMapping("/fee-config")
    public R<Map<String, Object>> getFeeConfig() {
        List<FeeConfig> list = feeConfigMapper.selectList(
                new LambdaQueryWrapper<FeeConfig>().orderByAsc(FeeConfig::getMinAmount));
        Map<String, Object> data = new HashMap<>();
        data.put("configs", list);
        data.put("updatedAt", System.currentTimeMillis());
        data.put("updatedByNickname", "系统");
        return R.ok(data);
    }

    @Operation(summary = "更新手续费配置（立即生效）")
    @PutMapping("/fee-config")
    public R<Void> updateFeeConfig(@RequestBody Map<String, Object> body) {
        Object configsObj = body.get("configs");
        if (!(configsObj instanceof List<?> rawList) || rawList.isEmpty()) {
            throw new BusinessException("手续费配置不能为空");
        }

        List<FeeConfig> configs = rawList.stream().map(item -> {
            if (!(item instanceof Map<?, ?> map)) {
                throw new BusinessException("手续费配置格式错误");
            }
            FeeConfig fee = new FeeConfig();
            fee.setMinAmount(new BigDecimal(map.get("minAmount").toString()));
            Object maxAmount = map.get("maxAmount");
            fee.setMaxAmount(maxAmount == null ? null : new BigDecimal(maxAmount.toString()));
            fee.setFeeRate(new BigDecimal(map.get("feeRate").toString()));
            Object isActive = map.get("isActive");
            if (isActive instanceof Boolean b) {
                fee.setIsActive(b ? 1 : 0);
            } else {
                fee.setIsActive(Integer.parseInt(isActive.toString()));
            }
            fee.setUpdatedBy(0L);
            return fee;
        }).toList();

        // 清空旧配置，插入新配置
        feeConfigMapper.delete(null);
        configs.forEach(feeConfigMapper::insert);
        // 刷新Redis缓存（动态手续费核心亮点）
        redisTemplate.delete("fee:config");
        return R.ok();
    }

    // ==================== 交易统计 ====================

    @Operation(summary = "全平台交易记录")
    @GetMapping("/trades")
    public R<Object> trades(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        LambdaQueryWrapper<com.campus.task.module.payment.entity.Settlement> wrapper = new LambdaQueryWrapper<com.campus.task.module.payment.entity.Settlement>()
                .orderByDesc(com.campus.task.module.payment.entity.Settlement::getSettledAt);
        if (startTime != null) {
            wrapper.ge(com.campus.task.module.payment.entity.Settlement::getSettledAt, toLocalDateTime(startTime));
        }
        if (endTime != null) {
            wrapper.le(com.campus.task.module.payment.entity.Settlement::getSettledAt, toLocalDateTime(endTime));
        }
        return R.ok(settlementMapper.selectPage(
                new Page<>(page, pageSize),
                wrapper));
    }

    @Operation(summary = "交易统计汇总")
    @GetMapping("/trades/stats")
    public R<Map<String, Object>> tradeStats(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        LambdaQueryWrapper<com.campus.task.module.payment.entity.Settlement> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(com.campus.task.module.payment.entity.Settlement::getSettledAt, toLocalDateTime(startTime));
        }
        if (endTime != null) {
            wrapper.le(com.campus.task.module.payment.entity.Settlement::getSettledAt, toLocalDateTime(endTime));
        }
        List<com.campus.task.module.payment.entity.Settlement> list = settlementMapper.selectList(wrapper);

        BigDecimal totalFeeIncome = BigDecimal.ZERO;
        BigDecimal totalTaskAmount = BigDecimal.ZERO;
        BigDecimal totalFeeRate = BigDecimal.ZERO;
        for (com.campus.task.module.payment.entity.Settlement s : list) {
            totalFeeIncome = totalFeeIncome.add(s.getFeeAmount() == null ? BigDecimal.ZERO : s.getFeeAmount());
            totalTaskAmount = totalTaskAmount.add(s.getTaskAmount() == null ? BigDecimal.ZERO : s.getTaskAmount());
            totalFeeRate = totalFeeRate.add(s.getFeeRate() == null ? BigDecimal.ZERO : s.getFeeRate());
        }

        BigDecimal avgFeeRate = list.isEmpty()
                ? BigDecimal.ZERO
                : totalFeeRate.divide(BigDecimal.valueOf(list.size()), 4, java.math.RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("totalFeeIncome", totalFeeIncome);
        result.put("totalTaskCount", list.size());
        result.put("totalTaskAmount", totalTaskAmount);
        result.put("avgFeeRate", avgFeeRate);
        result.put("statsByCategory", List.of());
        return R.ok(result);
    }

    @Operation(summary = "争议任务列表")
    @GetMapping("/disputes")
    public R<Page<Task>> disputes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 简化：已完成但验收不通过的任务视为争议
        Page<Task> p = taskMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, TaskStatus.COMPLETED.getCode())
                        .isNotNull(Task::getRejectReason)
                        .orderByDesc(Task::getUpdatedAt));
        return R.ok(p);
    }

    @Operation(summary = "处理争议")
    @PutMapping("/disputes/{taskId}/resolve")
    public R<Void> resolveDispute(@PathVariable Long taskId,
                                  @RequestBody Map<String, String> body) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        String decision = body.get("decision");
        if ("approve".equals(decision)) {
            // 支持发布者：取消任务退款
            task.setStatus(TaskStatus.CANCELLED.getCode());
        } else {
            // 支持接单者：直接结算
            task.setStatus(TaskStatus.SETTLED.getCode());
        }
        taskMapper.updateById(task);
        return R.ok();
    }

    // ==================== 分类标签管理 ====================

    @Operation(summary = "获取所有分类标签（含禁用）")
    @GetMapping("/categories")
    public R<List<TaskCategoryVO>> listCategories() {
        List<TaskCategory> list = taskCategoryMapper.selectList(
                new LambdaQueryWrapper<TaskCategory>().orderByAsc(TaskCategory::getSort));
        return R.ok(list.stream().map(c -> {
            TaskCategoryVO vo = new TaskCategoryVO();
            vo.setId(c.getId()); vo.setName(c.getName());
            vo.setIcon(c.getIcon()); vo.setSort(c.getSort());
            vo.setActive(c.getIsActive() == 1);
            return vo;
        }).toList());
    }

    @Operation(summary = "新增分类标签")
    @PostMapping("/categories")
    public R<Void> addCategory(@RequestBody Map<String, Object> body) {
        String name = body.get("name").toString();
        String icon = body.getOrDefault("icon", "🔧").toString();
        int sort = body.containsKey("sort") ? Integer.parseInt(body.get("sort").toString()) : 99;
        TaskCategory cat = new TaskCategory();
        cat.setName(name); cat.setIcon(icon); cat.setSort(sort); cat.setIsActive(1);
        taskCategoryMapper.insert(cat);
        redisTemplate.delete("category:list");
        return R.ok();
    }

    @Operation(summary = "修改分类标签")
    @PutMapping("/categories/{id}")
    public R<Void> updateCategory(@PathVariable Integer id,
                                   @RequestBody Map<String, Object> body) {
        TaskCategory cat = taskCategoryMapper.selectById(id);
        if (cat == null) throw new BusinessException("分类不存在");
        if (body.containsKey("name")) cat.setName(body.get("name").toString());
        if (body.containsKey("icon")) cat.setIcon(body.get("icon").toString());
        if (body.containsKey("sort")) cat.setSort(Integer.parseInt(body.get("sort").toString()));
        if (body.containsKey("isActive")) cat.setIsActive(Integer.parseInt(body.get("isActive").toString()));
        taskCategoryMapper.updateById(cat);
        redisTemplate.delete("category:list");
        return R.ok();
    }

    @Operation(summary = "删除分类标签")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Integer id) {
        taskCategoryMapper.deleteById(id);
        redisTemplate.delete("category:list");
        return R.ok();
    }

    private LocalDateTime toLocalDateTime(Long millis) {
        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
