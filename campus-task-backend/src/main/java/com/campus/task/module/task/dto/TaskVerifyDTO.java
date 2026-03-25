package com.campus.task.module.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 验收请求体
 */
@Data
public class TaskVerifyDTO {

    @NotNull(message = "验收结果不能为空")
    private Boolean pass;

    /** 不通过时必填 */
    private String rejectReason;
}
