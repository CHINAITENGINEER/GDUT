package com.campus.task.module.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐权重调整日志
 */
@Data
@TableName("recommend_weight_log")
public class RecommendWeightLog {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private Long taskId;

    /**
     * 触发类型：0手动初始化 1完成订单自动更新 2用户手动保存画像
     */
    private Integer triggerType;

    /** 调整前权重(JSON) */
    private String beforeWeights;

    /** 调整后权重(JSON) */
    private String afterWeights;

    /** 调整原因(JSON) */
    private String triggerReason;

    private LocalDateTime createdAt;
}
