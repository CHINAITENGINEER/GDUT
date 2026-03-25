package com.campus.task.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求体
 */
@Data
public class LoginDTO {

    /** 手机号或学号 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 密码（MD5加密），与smsCode二选一 */
    private String password;

    /** 短信验证码，与password二选一 */
    private String smsCode;

    /** 0=密码登录 1=验证码登录 */
    private Integer loginType = 0;

    /** 是否记住我（影响Token有效期） */
    private Boolean remember = false;
}
