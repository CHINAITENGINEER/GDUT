package com.campus.task.module.recommendation.vo;

import com.campus.task.module.task.vo.TaskCardVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 推荐任务VO
 */
@Data
public class RecommendedTaskVO {

    private TaskCardVO task;
    private BigDecimal score;
    private String recommendReason;
    private Map<String, BigDecimal> scoreBreakdown;
}
