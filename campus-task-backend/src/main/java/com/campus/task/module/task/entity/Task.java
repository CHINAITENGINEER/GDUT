package com.campus.task.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务实体
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long publisherId;
    private Long acceptorId;
    private String title;
    /** 分类：1代取快递 2资料整理 3编程 4代课占位 5其他 */
    private Integer category;
    private String description;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    /** 交付方式：0线上 1线下 */
    private Integer deliveryType;
    private LocalDateTime deadline;
    /**
     * 状态：0待审核 1待接单 2已抢单待协商 3待支付
     * 4进行中 5已完成 6已结算 7已互评 8已取消
     */
    private Integer status;
    /** 是否需要审核（金额>200自动置1） */
    private Integer needAudit;
    /** 任务图片列表（JSON数组） */
    private String taskImages;
    /** 交付成果文件列表（JSON数组） */
    private String deliveryProof;
    private String rejectReason;
    /** 锁单过期时间（抢单后+20分钟） */
    private LocalDateTime lockExpireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
