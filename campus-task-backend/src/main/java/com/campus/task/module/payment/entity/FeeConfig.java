package com.campus.task.module.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fee_config")
public class FeeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;  // null=无上限
    private BigDecimal feeRate;
    private Integer isActive;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
