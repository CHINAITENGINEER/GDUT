package com.campus.task.module.payment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付记录VO
 */
@Data
public class PaymentRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    private String taskTitle;
    private BigDecimal amount;
    /** 0=模拟支付宝 1=模拟微信 */
    private Integer payType;
    /** 0=待支付 1=已支付 2=已退款 */
    private Integer payStatus;
    private String tradeNo;
    private Long createdAt;
}
