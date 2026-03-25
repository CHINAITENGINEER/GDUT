package com.campus.task.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement")
public class Settlement {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long acceptorId;
    private BigDecimal taskAmount;
    private BigDecimal baseFeeRate;
    private Integer levelAtSettle;
    private BigDecimal feeDiscount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal realAmount;
    private Integer expGained;
    /** 0=已结算 1=已提现 */
    private Integer status;
    private LocalDateTime settledAt;
}
