package com.campus.task.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户余额VO
 */
@Data
public class BalanceVO {
    private BigDecimal balance;        // 可用余额
    private BigDecimal totalEarned;    // 累计收入
    private BigDecimal frozenAmount;  // 冻结金额
}
