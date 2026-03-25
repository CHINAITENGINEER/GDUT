package com.campus.task.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新个人资料请求体
 */
@Data
public class UserUpdateDTO {

    @Size(min = 2, max = 20, message = "昵称长度2-20字")
    private String nickname;

    /** 头像URL（先上传获得URL再提交） */
    private String avatar;

    @Size(max = 100, message = "简介最多100字")
    private String bio;

    /** 技能标签，最多10个 */
    private List<String> skills;
}
