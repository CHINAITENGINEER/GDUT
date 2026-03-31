package com.campus.task.module.recommendation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户推荐画像编辑请求
 */
@Data
public class RecommendationProfileDTO {

    /** 能力标签 */
    @Size(max = 12, message = "能力标签最多12个")
    private List<String> abilityTags;

    /** 偏好任务分类ID */
    @Size(max = 8, message = "偏好分类最多8个")
    private List<Integer> preferredCategoryIds;

    /** 偏好交付方式：0线上 1线下 */
    private Integer preferredDeliveryType;

    /** 可接受最低金额 */
    private java.math.BigDecimal minAcceptAmount;

    /** 可接受最高金额 */
    private java.math.BigDecimal maxAcceptAmount;

    /** 每日最多推荐数量 */
    private Integer dailyRecommendLimit;


    /** 推荐权重（JSON对象） */
    private java.util.Map<String, java.math.BigDecimal> weights;
}
