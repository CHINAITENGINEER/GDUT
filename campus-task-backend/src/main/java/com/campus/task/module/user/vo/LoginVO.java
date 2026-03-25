package com.campus.task.module.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 登录返回VO
 */
@Data
public class LoginVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer level;
    private Integer creditScore;
    private String token;
}
