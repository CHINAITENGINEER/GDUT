package com.campus.task.module.task.vo;

import lombok.Data;

/**
 * 任务分类标签VO（返回给前端）
 */
@Data
public class TaskCategoryVO {
    private Integer id;
    private String name;
    private String icon;
    private Integer sort;
    private Boolean active;
}
