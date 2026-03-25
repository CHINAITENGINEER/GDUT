package com.campus.task.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户违规记录
 */
@Data
@TableName("user_violation")
public class UserViolation {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private Long taskId;
    /**
     * 违规类型：1超时未确认 2抢单后主动取消 3协商超时
     */
    private Integer violationType;
    private LocalDate violationDate;
    private LocalDateTime createdAt;
}
