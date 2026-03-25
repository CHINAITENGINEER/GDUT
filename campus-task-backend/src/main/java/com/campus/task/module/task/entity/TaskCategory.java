package com.campus.task.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务分类标签实体
 */
@Data
@TableName("task_category")
public class TaskCategory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 分类名称 */
    private String name;

    /** 图标（emoji 或 icon class） */
    private String icon;

    /** 排序权重，越小越靠前 */
    private Integer sort;

    /** 是否启用：0禁用 1启用 */
    private Integer isActive;

    private LocalDateTime createdAt;
}
