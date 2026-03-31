package com.campus.task.module.recommendation.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 推荐画像VO
 */
@Data
public class RecommendationProfileVO {

    private List<String> abilityTags;
    private List<Integer> preferredCategoryIds;
    private Integer preferredDeliveryType;
    private BigDecimal minAcceptAmount;
    private BigDecimal maxAcceptAmount;
    private Integer dailyRecommendLimit;
    private Map<String, BigDecimal> weights;
}
