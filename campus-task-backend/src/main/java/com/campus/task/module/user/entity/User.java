package com.campus.task.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String studentId;
    private String phone;
    private String password;
    private String salt;
    private String nickname;
    private String avatar;
    private String bio;
    /** 技能标签（JSON数组字符串） */
    private String skills;
    /** 角色：0普通用户 1管理员 */
    private Integer role;
    /** 信誉分 */
    private Integer creditScore;
    /** 账户可提现余额 */
    private BigDecimal balance;
    /** 历史总收入（只增不减） */
    private BigDecimal totalEarned;
    /** 经验值 */
    private Integer exp;
    /** 接单等级 1-6 */
    private Integer level;
    /** 状态：0正常 1禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
