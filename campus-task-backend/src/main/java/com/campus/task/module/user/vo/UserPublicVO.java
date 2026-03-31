package com.campus.task.module.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 他人主页VO（公开信息）
 */
@Data
public class UserPublicVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
    private List<String> skills;
    private List<String> abilityTags;
    private List<Integer> preferredCategoryIds;
    private Integer preferredDeliveryType;
    private Integer maxDistanceKm;
    private Integer creditScore;
    private Integer level;
    private String levelName;
    private Double feeDiscount;
    private BigDecimal totalEarned;
    private Integer completedCount;
    private Long createdAt;
}
