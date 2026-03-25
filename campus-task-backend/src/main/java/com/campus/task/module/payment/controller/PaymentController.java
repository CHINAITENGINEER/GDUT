package com.campus.task.module.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.enums.TaskStatus;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.R;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.payment.entity.Payment;
import com.campus.task.module.payment.entity.Settlement;
import com.campus.task.module.payment.mapper.PaymentMapper;
import com.campus.task.module.payment.mapper.SettlementMapper;
import com.campus.task.module.payment.vo.PaymentRecordVO;
import com.campus.task.module.payment.vo.SettlementRecordVO;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.mapper.TaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Tag(name = "支付结算")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentMapper paymentMapper;
    private final SettlementMapper settlementMapper;
    private final TaskMapper taskMapper;
    private final SnowflakeUtil snowflakeUtil;

    @Operation(summary = "模拟支付")
    @PostMapping("/api/payment/pay")
    public R<Object> pay(@AuthenticationPrincipal UserDetails user,
                         @RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        Integer payType = Integer.valueOf(body.get("payType").toString());
        Long userId = Long.valueOf(user.getUsername());

        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (!userId.equals(task.getPublisherId())) throw new BusinessException("无权操作");
        if (task.getStatus() != TaskStatus.PENDING_PAYMENT.getCode())
            throw new BusinessException("任务不在待支付状态");

        String tradeNo = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Payment payment = new Payment();
        payment.setId(snowflakeUtil.nextId());
        payment.setTaskId(taskId);
        payment.setPayerId(userId);
        payment.setAmount(task.getAmount());
        payment.setPayType(payType);
        payment.setPayStatus(1);
        payment.setTradeNo(tradeNo);
        paymentMapper.insert(payment);

        // 支付成功后任务变为进行中
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        taskMapper.updateById(task);

        return R.ok(Map.of("tradeNo", tradeNo, "payStatus", 1, "message", "支付成功"));
    }

    @Operation(summary = "我的支付记录")
    @GetMapping("/api/payment/records")
    public R<Page<PaymentRecordVO>> payRecords(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = Long.valueOf(user.getUsername());
        Page<Payment> p = paymentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPayerId, userId)
                        .orderByDesc(Payment::getCreatedAt));
        Page<PaymentRecordVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(pay -> {
            PaymentRecordVO vo = new PaymentRecordVO();
            vo.setId(pay.getId());
            vo.setTaskId(pay.getTaskId());
            Task t = taskMapper.selectById(pay.getTaskId());
            vo.setTaskTitle(t != null ? t.getTitle() : "");
            vo.setAmount(pay.getAmount());
            vo.setPayType(pay.getPayType());
            vo.setPayStatus(pay.getPayStatus());
            vo.setTradeNo(pay.getTradeNo());
            vo.setCreatedAt(pay.getCreatedAt()
                    .atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
            return vo;
        }).toList());
        return R.ok(result);
    }

    @Operation(summary = "我的结算记录")
    @GetMapping("/api/settlement/records")
    public R<Page<SettlementRecordVO>> settlementRecords(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = Long.valueOf(user.getUsername());
        Page<Settlement> p = settlementMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getAcceptorId, userId)
                        .orderByDesc(Settlement::getSettledAt));
        Page<SettlementRecordVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(s -> {
            SettlementRecordVO vo = new SettlementRecordVO();
            vo.setId(s.getId());
            vo.setTaskId(s.getTaskId());
            Task t = taskMapper.selectById(s.getTaskId());
            vo.setTaskTitle(t != null ? t.getTitle() : "");
            vo.setTaskAmount(s.getTaskAmount());
            vo.setBaseFeeRate(s.getBaseFeeRate());
            vo.setLevelAtSettle(s.getLevelAtSettle());
            vo.setFeeDiscount(s.getFeeDiscount());
            vo.setFeeRate(s.getFeeRate());
            vo.setFeeAmount(s.getFeeAmount());
            vo.setRealAmount(s.getRealAmount());
            vo.setExpGained(s.getExpGained());
            vo.setStatus(s.getStatus());
            vo.setSettledAt(s.getSettledAt()
                    .atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
            return vo;
        }).toList());
        return R.ok(result);
    }

    @Operation(summary = "申请提现")
    @PostMapping("/api/settlement/withdraw")
    public R<Object> withdraw(@AuthenticationPrincipal UserDetails user,
                              @RequestBody Map<String, Object> body) {
        // 比赛演示：模拟提现成功
        return R.ok(Map.of("message", "提现申请已提交，请等待管理员审核"));
    }
}
