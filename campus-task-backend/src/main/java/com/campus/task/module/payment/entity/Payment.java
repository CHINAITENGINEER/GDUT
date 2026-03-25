package com.campus.task.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long payerId;
    private BigDecimal amount;
    /** 0=模拟支付宝 1=模拟微信 */
    private Integer payType;
    /** 0=待支付 1=已支付 2=已退款 */
    private Integer payStatus;
    private String tradeNo;
    private LocalDateTime createdAt;
}
