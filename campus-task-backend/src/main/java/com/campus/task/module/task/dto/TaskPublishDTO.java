package com.campus.task.module.task.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发布任务请求体
 */
@Data
public class TaskPublishDTO {

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度2-100字")
    private String title;

    @NotNull(message = "任务分类不能为空")
    @Min(value = 1, message = "分类ID必须大于0")
    private Integer category;

    @NotBlank(message = "任务描述不能为空")
    @Size(min = 10, max = 1000, message = "描述长度10-1000字")
    private String description;

    @NotNull(message = "任务金额不能为空")
    @DecimalMin(value = "1.00", message = "金额最低1元")
    @DecimalMax(value = "500.00", message = "金额最高500元")
    private BigDecimal amount;

    @NotNull(message = "交付方式不能为空")
    private Integer deliveryType;

    /** 截止时间（毫秒时间戳） */
    @NotNull(message = "截止时间不能为空")
    private Long deadline;

    /** 任务图片URL数组（先上传再填入，最多9张） */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> taskImages;
}
