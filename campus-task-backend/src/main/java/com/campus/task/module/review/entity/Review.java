package com.campus.task.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long reviewerId;
    private Long revieweeId;
    /** 星级 1-5 */
    private Integer score;
    private String content;
    /** 0=发布者评接单者 1=接单者评发布者 */
    private Integer type;
    private LocalDateTime createdAt;
}
