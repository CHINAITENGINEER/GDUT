package com.campus.task.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.enums.TaskStatus;
import com.campus.task.common.enums.UserLevel;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.ResultCode;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.message.service.MessageService;
import com.campus.task.module.payment.entity.Settlement;
import com.campus.task.module.payment.mapper.SettlementMapper;
import com.campus.task.module.recommendation.service.RecommendationService;
import com.campus.task.module.task.dto.*;
import com.campus.task.module.task.entity.GrabRecord;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.entity.UserViolation;
import com.campus.task.module.task.mapper.GrabRecordMapper;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.task.mapper.UserViolationMapper;
import com.campus.task.module.task.service.TaskService;
import com.campus.task.module.task.vo.*;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final GrabRecordMapper grabRecordMapper;
    private final UserViolationMapper userViolationMapper;
    private final SettlementMapper settlementMapper;
    private final SnowflakeUtil snowflakeUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final RecommendationService recommendationService;
    private final com.campus.task.module.task.mapper.TaskCategoryMapper taskCategoryMapper;

    @Value("${platform.task-audit-threshold:200}")
    private double auditThreshold;

    @Value("${platform.grab-lock-minutes:20}")
    private int grabLockMinutes;

    @Value("${platform.grab-daily-limit:5}")
    private int grabDailyLimit;

    @Value("${platform.violation-daily-limit:3}")
    private int violationDailyLimit;

    // ==================== 发布任务 ====================

    @Override
    @Transactional
    public TaskDetailVO publish(Long publisherId, TaskPublishDTO dto) {
        Task task = new Task();
        task.setId(snowflakeUtil.nextId());
        task.setPublisherId(publisherId);
        task.setTitle(dto.getTitle());
        task.setCategory(dto.getCategory());
        task.setDescription(dto.getDescription());
        task.setAmount(dto.getAmount());
        task.setDeliveryType(dto.getDeliveryType());
        task.setDeadline(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(dto.getDeadline()), ZoneId.of("Asia/Shanghai")));
        task.setTaskImages(toJson(dto.getTaskImages()));
        boolean needAudit = dto.getAmount().doubleValue() > auditThreshold;
        task.setNeedAudit(needAudit ? 1 : 0);
        // 发布任务后：如果需要审核则待审核(0)，否则直接进入待接单(1)
        task.setStatus(needAudit ? TaskStatus.PENDING_AUDIT.getCode() : TaskStatus.PENDING_GRAB.getCode());
        taskMapper.insert(task);
        return detail(task.getId(), publisherId);
    }

    // ==================== 任务列表 ====================

    @Override
    public Page<TaskCardVO> list(TaskQueryDTO dto) {
        Page<Task> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, TaskStatus.PENDING_GRAB.getCode());
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(Task::getTitle, dto.getKeyword())
                    .or().like(Task::getDescription, dto.getKeyword()));
        }
        if (dto.getCategory() != null) wrapper.eq(Task::getCategory, dto.getCategory());
        if (dto.getDeliveryType() != null) wrapper.eq(Task::getDeliveryType, dto.getDeliveryType());
        if (dto.getMinAmount() != null) wrapper.ge(Task::getAmount, dto.getMinAmount());
        if (dto.getMaxAmount() != null) wrapper.le(Task::getAmount, dto.getMaxAmount());
        if ("amount_asc".equals(dto.getSortBy())) wrapper.orderByAsc(Task::getAmount);
        else if ("amount_desc".equals(dto.getSortBy())) wrapper.orderByDesc(Task::getAmount);
        else wrapper.orderByDesc(Task::getCreatedAt);
        taskMapper.selectPage(page, wrapper);
        Page<TaskCardVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toCardVO).toList());
        return result;
    }

    // ==================== 任务详情 ====================

    @Override
    public TaskDetailVO detail(Long taskId, Long currentUserId) {
        Task task = getTask(taskId);
        User publisher = userMapper.selectById(task.getPublisherId());
        TaskDetailVO vo = new TaskDetailVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setCategory(task.getCategory());
        vo.setCategoryName(getCategoryName(task.getCategory()));
        vo.setDescription(task.getDescription());
        vo.setAmount(task.getAmount());
        vo.setDeliveryType(task.getDeliveryType());
        vo.setDeadline(toTimestamp(task.getDeadline()));
        vo.setStatus(task.getStatus());
        vo.setStatusName(TaskStatus.of(task.getStatus()).getDesc());
        vo.setNeedAudit(task.getNeedAudit() == 1);
        vo.setTaskImages(fromJson(task.getTaskImages()));
        vo.setDeliveryProof(fromJson(task.getDeliveryProof()));
        vo.setRejectReason(task.getRejectReason());
        if (task.getLockExpireAt() != null) vo.setLockExpireAt(toTimestamp(task.getLockExpireAt()));
        vo.setPublisher(toUserInfo(publisher));
        if (task.getAcceptorId() != null) {
            User acceptor = userMapper.selectById(task.getAcceptorId());
            if (acceptor != null) {
                vo.setAcceptor(toUserInfo(acceptor));
                UserLevel lv = UserLevel.of(acceptor.getLevel());
                BigDecimal baseFeeRate = getBaseFeeRate(task.getAmount());
                BigDecimal feeRate = baseFeeRate.multiply(BigDecimal.valueOf(lv.getFeeDiscount()))
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal fee = task.getAmount().multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
                vo.setEstimatedFee(fee);
                vo.setEstimatedIncome(task.getAmount().subtract(fee));
            }
        }

        // 聊天室使用 grabRecordId 隔离不同“接单轮次”，这里为当前用户返回可用的轮次ID。
        // 只有任务发布者或当前接单者，才返回 grabRecordId；其他用户返回 null。
        if (currentUserId != null && task.getAcceptorId() != null) {
            boolean canChat = currentUserId.equals(task.getPublisherId())
                    || currentUserId.equals(task.getAcceptorId());
            if (canChat) {
                GrabRecord record = grabRecordMapper.selectOne(new LambdaQueryWrapper<GrabRecord>()
                        .eq(GrabRecord::getTaskId, taskId)
                        .eq(GrabRecord::getUserId, task.getAcceptorId())
                        .in(GrabRecord::getStatus, Arrays.asList(0, 1))
                        .orderByDesc(GrabRecord::getCreatedAt)
                        .last("LIMIT 1"));
                vo.setGrabRecordId(record != null ? record.getId() : null);
            }
        }
        vo.setCreatedAt(toTimestamp(task.getCreatedAt()));
        vo.setUpdatedAt(toTimestamp(task.getUpdatedAt()));
        return vo;
    }

    // ==================== 抢单流程 ====================

    @Override
    @Transactional
    public GrabVO grab(Long taskId, Long userId) {
        Task task = getTask(taskId);
        if (task.getStatus() != TaskStatus.PENDING_GRAB.getCode())
            throw new BusinessException(409, "任务当前不可抢单");
        LocalDate today = LocalDate.now();
        int dailyCount = grabRecordMapper.countDailyGrab(userId, today);
        if (dailyCount >= grabDailyLimit)
            throw new BusinessException(409, "今日抢单次数已达上限(" + grabDailyLimit + "次)");
        if (grabRecordMapper.countTaskGrab(userId, taskId, today) > 0)
            throw new BusinessException(409, "今日已抢过该任务");
        int violations = userViolationMapper.countDailyViolation(userId, today);
        if (violations >= violationDailyLimit)
            throw new BusinessException(409, "今日违规次数过多，明日可继续");
        User user = userMapper.selectById(userId);
        if (user.getCreditScore() < 1)
            throw new BusinessException(409, "积分不足，无法抢单");
        user.setCreditScore(user.getCreditScore() - 1);
        userMapper.updateById(user);
        task.setAcceptorId(userId);
        task.setStatus(TaskStatus.GRABBED.getCode());
        LocalDateTime lockExpire = LocalDateTime.now().plusMinutes(grabLockMinutes);
        task.setLockExpireAt(lockExpire);
        taskMapper.updateById(task);
        GrabRecord record = new GrabRecord();
        record.setId(snowflakeUtil.nextId());
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setGrabDate(today);
        record.setStatus(0);
        record.setIsViolation(0);
        grabRecordMapper.insert(record);
        messageService.pushSystem(task.getPublisherId(), taskId,
                "有人抢单了您的任务「" + task.getTitle() + "」，请在" + grabLockMinutes + "分钟内前往任务详情确认或拒绝，双方可在详情页协商任务细节。");
        log.info("用户[{}]抢单任务[{}]成功，锁单至{}", userId, taskId, lockExpire);
        GrabVO vo = new GrabVO();
        vo.setLockExpireAt(toTimestamp(lockExpire));
        vo.setMessage("抢单成功！请等待发布者确认，剩余" + grabLockMinutes + "分钟");
        vo.setGrabRecordId(String.valueOf(record.getId()));
        return vo;
    }

    @Override
    @Transactional
    public GrabConfirmVO confirmGrab(Long taskId, Long publisherId) {
        Task task = getTask(taskId);
        if (!publisherId.equals(task.getPublisherId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.GRABBED.getCode())
            throw new BusinessException("任务状态不支持此操作");
        task.setStatus(TaskStatus.PENDING_PAYMENT.getCode());
        task.setLockExpireAt(null);
        taskMapper.updateById(task);
        updateGrabRecord(taskId, task.getAcceptorId(), 1);
        messageService.pushSystem(task.getAcceptorId(), taskId,
                "发布者已确认接受您接单「" + task.getTitle() + "」，请前往任务详情页与发布者完成协商，等待发布者付款后任务开始。");
        GrabConfirmVO vo = new GrabConfirmVO();
        vo.setPayUrl("/payment/pay?taskId=" + taskId);
        vo.setMessage("已确认接单者，请完成付款以开始任务");
        return vo;
    }

    @Override
    @Transactional
    public void rejectGrab(Long taskId, Long publisherId) {
        Task task = getTask(taskId);
        if (!publisherId.equals(task.getPublisherId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.GRABBED.getCode())
            throw new BusinessException("任务状态不支持此操作");
        Long acceptorId = task.getAcceptorId();
        returnCreditScore(acceptorId, 1);
        releaseTask(task);
        updateGrabRecord(taskId, acceptorId, 2);
        messageService.pushSystem(acceptorId, taskId,
                "发布者已拒绝您接单「" + task.getTitle() + "」，已退还押金，任务重新上架。");
    }

    @Override
    @Transactional
    public void cancelGrab(Long taskId, Long userId) {
        Task task = getTask(taskId);
        if (!userId.equals(task.getAcceptorId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.GRABBED.getCode())
            throw new BusinessException("任务状态不支持此操作");
        recordViolation(userId, taskId, 2);
        deductExp(userId, 20);
        releaseTask(task);
        updateGrabRecord(taskId, userId, 4);
        messageService.pushSystem(task.getPublisherId(), taskId,
                "接单者已主动取消接单「" + task.getTitle() + "」，任务重新上架。");
    }

    @Override
    @Transactional
    public void submit(Long taskId, Long acceptorId, TaskSubmitDTO dto) {
        Task task = getTask(taskId);
        if (!acceptorId.equals(task.getAcceptorId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.IN_PROGRESS.getCode())
            throw new BusinessException("任务不在进行中状态");
        if (dto.getProofUrls() == null || dto.getProofUrls().isEmpty())
            throw new BusinessException("至少上传1个交付凭证");
        task.setDeliveryProof(toJson(dto.getProofUrls()));
        task.setStatus(TaskStatus.COMPLETED.getCode());
        taskMapper.updateById(task);
        messageService.pushSystem(task.getPublisherId(), taskId,
                "接单者已提交成果，请验收任务「" + task.getTitle() + "」。");
    }

    @Override
    @Transactional
    public VerifyVO verify(Long taskId, Long publisherId, TaskVerifyDTO dto) {
        Task task = getTask(taskId);
        if (!publisherId.equals(task.getPublisherId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.COMPLETED.getCode())
            throw new BusinessException("任务未处于已完成状态");
        VerifyVO vo = new VerifyVO();
        if (Boolean.TRUE.equals(dto.getPass())) {
            task.setStatus(TaskStatus.SETTLED.getCode());
            taskMapper.updateById(task);
            VerifyVO.Settlement settlementInfo = settle(task);
            vo.setMessage("验收通过，结算已完成");
            vo.setSettlement(settlementInfo);
        } else {
            if (!StringUtils.hasText(dto.getRejectReason()))
                throw new BusinessException("验收不通过需填写原因");
            task.setRejectReason(dto.getRejectReason());
            task.setStatus(TaskStatus.IN_PROGRESS.getCode());
            taskMapper.updateById(task);
            messageService.pushSystem(task.getAcceptorId(), taskId,
                    "发布者验收不通过「" + task.getTitle() + "」，原因：" + dto.getRejectReason() + "，请修改后重新提交。");
            vo.setMessage("已驳回，等待接单者重新提交");
        }
        return vo;
    }

    @Override
    @Transactional
    public void cancel(Long taskId, Long publisherId) {
        Task task = getTask(taskId);
        if (!publisherId.equals(task.getPublisherId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        if (task.getStatus() != TaskStatus.PENDING_GRAB.getCode())
            throw new BusinessException("仅待接单状态可取消");
        task.setStatus(TaskStatus.CANCELLED.getCode());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void dispute(Long taskId, Long userId, String reason) {
        Task task = getTask(taskId);
        if (!userId.equals(task.getPublisherId()) && !userId.equals(task.getAcceptorId()))
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作");
        log.info("任务[{}]申请管理员介入，申请人[{}]，原因:{}", taskId, userId, reason);
        task.setRejectReason("[争议]" + reason);
        taskMapper.updateById(task);
    }

    @Override
    public Page<TaskCardVO> myPublished(Long userId, Integer page, Integer pageSize, Integer status) {
        Page<Task> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, userId)
                .orderByDesc(Task::getCreatedAt);
        if (status != null) wrapper.eq(Task::getStatus, status);
        taskMapper.selectPage(p, wrapper);
        Page<TaskCardVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toCardVO).toList());
        return result;
    }

    @Override
    public Page<TaskCardVO> myGrabbed(Long userId, Integer page, Integer pageSize, Integer status) {
        Page<Task> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getAcceptorId, userId)
                .orderByDesc(Task::getCreatedAt);
        if (status != null) wrapper.eq(Task::getStatus, status);
        taskMapper.selectPage(p, wrapper);
        Page<TaskCardVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toCardVO).toList());
        return result;
    }

    @Override
    @Transactional
    public void handleExpiredGrabs() {
        List<Task> expired = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, TaskStatus.GRABBED.getCode())
                .le(Task::getLockExpireAt, LocalDateTime.now()));
        for (Task task : expired) {
            Long acceptorId = task.getAcceptorId();
            recordViolation(acceptorId, task.getId(), 3);
            deductExp(acceptorId, 20);
            releaseTask(task);
            updateGrabRecord(task.getId(), acceptorId, 3);
            messageService.pushSystem(acceptorId, task.getId(),
                    "您接单「" + task.getTitle() + "」已超时（20分钟内未完成协商），押金已扣除并记一次违规。");
            messageService.pushSystem(task.getPublisherId(), task.getId(),
                    "接单者协商超时，任务「" + task.getTitle() + "」已自动重新上架。");
            log.info("任务[{}]锁单超时，自动释放，接单者[{}]记违规", task.getId(), acceptorId);
        }
    }

    // ==================== 结算（写入settlement表） ====================

    private VerifyVO.Settlement settle(Task task) {
        User acceptor = userMapper.selectById(task.getAcceptorId());
        UserLevel lv = UserLevel.of(acceptor.getLevel());
        BigDecimal baseFeeRate = getBaseFeeRate(task.getAmount());
        BigDecimal feeRate = baseFeeRate.multiply(BigDecimal.valueOf(lv.getFeeDiscount()))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal feeAmount = task.getAmount().multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal realAmount = task.getAmount().subtract(feeAmount);
        task.setFeeAmount(feeAmount);
        taskMapper.updateById(task);
        int expGained = (int) (task.getAmount().doubleValue() * 2);
        int newExp = acceptor.getExp() + expGained;
        UserLevel newLevel = UserLevel.calcLevel(newExp);
        boolean levelUp = newLevel.getLevel() > acceptor.getLevel();
        acceptor.setBalance(acceptor.getBalance().add(realAmount));
        acceptor.setTotalEarned(acceptor.getTotalEarned().add(realAmount));
        acceptor.setExp(newExp);
        acceptor.setLevel(newLevel.getLevel());
        acceptor.setCreditScore(acceptor.getCreditScore() + 1);
        userMapper.updateById(acceptor);
        Settlement settlement = new Settlement();
        settlement.setId(snowflakeUtil.nextId());
        settlement.setTaskId(task.getId());
        settlement.setAcceptorId(acceptor.getId());
        settlement.setTaskAmount(task.getAmount());
        settlement.setBaseFeeRate(baseFeeRate);
        settlement.setLevelAtSettle(lv.getLevel());
        settlement.setFeeDiscount(BigDecimal.valueOf(lv.getFeeDiscount()));
        settlement.setFeeRate(feeRate);
        settlement.setFeeAmount(feeAmount);
        settlement.setRealAmount(realAmount);
        settlement.setExpGained(expGained);
        settlement.setStatus(0);
        settlementMapper.insert(settlement);
        recommendationService.updateWeightsAfterSettlement(acceptor.getId(), task.getId());
        redisTemplate.delete("user:level:" + acceptor.getId());
        redisTemplate.delete("user:credit:" + acceptor.getId());
        if (levelUp) {
            messageService.pushSystem(acceptor.getId(),
                    "恭喜！你已升至 Lv." + newLevel.getLevel() + "（" + newLevel.getLevelName() +
                    "），手续费折扣提升为×" + newLevel.getFeeDiscount() + "！");
        }
        messageService.pushSystem(acceptor.getId(), task.getId(),
                "任务「" + task.getTitle() + "」已结算，到账 ¥" + realAmount + "，获得 " + expGained + " 经验值。");
        log.info("任务[{}]结算：金额={}, 手续费={}, 到账={}, 经验+{}, 等级={}",
                task.getId(), task.getAmount(), feeAmount, realAmount, expGained, newLevel.getLevelName());
        VerifyVO.Settlement info = new VerifyVO.Settlement();
        info.setTaskAmount(task.getAmount());
        info.setFeeDiscount(lv.getFeeDiscount());
        info.setFeeAmount(feeAmount);
        info.setRealAmount(realAmount);
        info.setExpGained(expGained);
        return info;
    }

    // ==================== 私有辅助 ====================

    private Task getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "任务不存在");
        return task;
    }

    private BigDecimal getBaseFeeRate(BigDecimal amount) {
        return amount.compareTo(BigDecimal.valueOf(100)) < 0
                ? BigDecimal.valueOf(0.05) : BigDecimal.valueOf(0.03);
    }

    private void releaseTask(Task task) {
        task.setAcceptorId(null);
        task.setStatus(TaskStatus.PENDING_GRAB.getCode());
        task.setLockExpireAt(null);
        taskMapper.updateById(task);
    }

    private void returnCreditScore(Long userId, int amount) {
        User user = userMapper.selectById(userId);
        user.setCreditScore(user.getCreditScore() + amount);
        userMapper.updateById(user);
    }

    private void deductExp(Long userId, int amount) {
        User user = userMapper.selectById(userId);
        UserLevel currentLv = UserLevel.of(user.getLevel());
        int newExp = Math.max(currentLv.getRequiredExp(), user.getExp() - amount);
        user.setExp(newExp);
        userMapper.updateById(user);
    }

    private void recordViolation(Long userId, Long taskId, int type) {
        UserViolation v = new UserViolation();
        v.setId(snowflakeUtil.nextId());
        v.setUserId(userId);
        v.setTaskId(taskId);
        v.setViolationType(type);
        v.setViolationDate(LocalDate.now());
        userViolationMapper.insert(v);
    }

    private void updateGrabRecord(Long taskId, Long userId, int status) {
        GrabRecord record = grabRecordMapper.selectOne(new LambdaQueryWrapper<GrabRecord>()
                .eq(GrabRecord::getTaskId, taskId)
                .eq(GrabRecord::getUserId, userId)
                .orderByDesc(GrabRecord::getCreatedAt)
                .last("LIMIT 1"));
        if (record != null) {
            record.setStatus(status);
            record.setIsViolation(status == 3 || status == 4 ? 1 : 0);
            grabRecordMapper.updateById(record);
        }
    }

    private TaskCardVO toCardVO(Task task) {
        TaskCardVO vo = new TaskCardVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setCategory(task.getCategory());
        vo.setCategoryName(getCategoryName(task.getCategory()));
        vo.setAmount(task.getAmount());
        vo.setDeliveryType(task.getDeliveryType());
        vo.setDeadline(toTimestamp(task.getDeadline()));
        vo.setStatus(task.getStatus());
        vo.setStatusName(TaskStatus.of(task.getStatus()).getDesc());
        vo.setTaskImages(fromJson(task.getTaskImages()));
        vo.setCreatedAt(toTimestamp(task.getCreatedAt()));
        User publisher = userMapper.selectById(task.getPublisherId());
        TaskCardVO.PublisherInfo pi = new TaskCardVO.PublisherInfo();
        if (publisher != null) {
            pi.setId(publisher.getId());
            pi.setNickname(publisher.getNickname());
            pi.setAvatar(publisher.getAvatar());
            pi.setCreditScore(publisher.getCreditScore());
        } else {
            // 防御性编程：如果发布者不存在，设置默认值
            pi.setId(task.getPublisherId());
            pi.setNickname("未知用户");
            pi.setAvatar(null);
            pi.setCreditScore(0);
        }
        vo.setPublisher(pi);
        return vo;
    }

    private TaskDetailVO.UserInfo toUserInfo(User user) {
        if (user == null) return null;
        TaskDetailVO.UserInfo info = new TaskDetailVO.UserInfo();
        info.setId(user.getId());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setCreditScore(user.getCreditScore());
        info.setLevel(user.getLevel());
        UserLevel lv = UserLevel.of(user.getLevel());
        info.setLevelName(lv.getLevelName());
        info.setFeeDiscount(lv.getFeeDiscount());
        return info;
    }

    private long toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return 0;
        return ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return null; }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    private String getCategoryName(Integer categoryId) {
        if (categoryId == null) return "其他";
        com.campus.task.module.task.entity.TaskCategory cat =
                taskCategoryMapper.selectById(categoryId);
        return cat != null ? cat.getName() : "其他";
    }
}
 