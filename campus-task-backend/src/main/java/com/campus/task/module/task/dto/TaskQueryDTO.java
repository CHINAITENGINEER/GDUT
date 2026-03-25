package com.campus.task.module.task.dto;

import lombok.Data;

/**
 * 任务列表查询参数
 */
@Data
public class TaskQueryDTO {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    /** 分类ID（动态标签，对应 task_category.id） */
    private Integer category;
    private Double minAmount;
    private Double maxAmount;
    private Integer deliveryType;
    /** newest / amount_asc / amount_desc */
    private String sortBy = "newest";
}
