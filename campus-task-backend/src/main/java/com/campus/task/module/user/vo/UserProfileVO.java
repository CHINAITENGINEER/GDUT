package com.campus.task.module.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户个人信息VO
 */
@Data
public class UserProfileVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String studentId;
    private String phone;         // 脱敏
    private String nickname;
    private String avatar;
    private String bio;
    private List<String> skills;
    private List<String> abilityTags;
    private List<Integer> preferredCategoryIds;
    private Integer preferredDeliveryType;
    private java.math.BigDecimal minAcceptAmount;
    private java.math.BigDecimal maxAcceptAmount;
    private Integer maxDistanceKm;
    private Integer dailyRecommendLimit;
    private java.util.Map<String, java.math.BigDecimal> recommendWeights;
    private Integer role;
    private String currentRole;   // publisher / acceptor
    private Integer creditScore;
    private Integer level;
    private String levelName;
    private Integer exp;
    private Integer nextLevelExp;
    private Integer expToNext;
    private Integer progress;     // 0-100
    private Double feeDiscount;
    private BigDecimal balance;
    private BigDecimal totalEarned;
    private Integer status;
    private Long createdAt;       // 毫秒时间戳
}
