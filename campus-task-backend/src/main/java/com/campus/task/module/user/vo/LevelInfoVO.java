package com.campus.task.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 等级详情VO
 */
@Data
public class LevelInfoVO {
    private Integer level;
    private String levelName;
    private Integer exp;
    private Integer nextLevelExp;  // null表示已满级
    private Integer expToNext;     // 满级时为0
    private Integer progress;      // 0-100，满级为100
    private Double feeDiscount;
    private BigDecimal totalEarned;
}
