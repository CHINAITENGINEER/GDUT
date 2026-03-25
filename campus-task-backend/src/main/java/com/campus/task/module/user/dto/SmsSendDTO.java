package com.campus.task.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送短信验证码请求体
 */
@Data
public class SmsSendDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 使用场景：register / login / reset */
    @NotBlank(message = "场景不能为空")
    private String scene;
}
