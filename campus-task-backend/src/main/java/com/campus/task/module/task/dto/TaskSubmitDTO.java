package com.campus.task.module.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交交付成果请求体
 */
@Data
public class TaskSubmitDTO {

    @NotEmpty(message = "至少上传一个交付凭证")
    @Size(max = 9, message = "最多上传9个文件")
    private List<String> proofUrls;

    @Size(max = 200, message = "备注最多200字")
    private String remark;
}
