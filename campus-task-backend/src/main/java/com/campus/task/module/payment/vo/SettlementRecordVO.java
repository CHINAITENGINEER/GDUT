package com.campus.task.module.payment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算记录VO
 */
@Data
public class SettlementRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    private String taskTitle;
    private BigDecimal taskAmount;
    private BigDecimal baseFeeRate;
    /** 结算时接单者等级快照 */
    private Integer levelAtSettle;
    /** 等级折扣率快照，如 0.90 */
    private BigDecimal feeDiscount;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal realAmount;
    private Integer expGained;
    /** 0=已结算 1=已提现 */
    private Integer status;
    private Long settledAt;
}
