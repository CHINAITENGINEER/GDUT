package com.campus.task.module.task.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 验收返回VO
 */
@Data
public class VerifyVO {
    private String message;
    /** 验收通过时才有值 */
    private Settlement settlement;

    @Data
    public static class Settlement {
        private BigDecimal taskAmount;
        /** 接单者等级折扣率，如 0.90 */
        private Double feeDiscount;
        private BigDecimal feeAmount;
        private BigDecimal realAmount;
        private Integer expGained;
    }
}
