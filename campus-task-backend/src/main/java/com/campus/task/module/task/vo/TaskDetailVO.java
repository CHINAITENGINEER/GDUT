package com.campus.task.module.task.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 任务详情VO
 */
@Data
public class TaskDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private Integer category;
    private String categoryName;
    private String description;
    private BigDecimal amount;
    private Integer deliveryType;
    private Long deadline;
    private Integer status;
    private String statusName;
    private Boolean needAudit;
    private List<String> taskImages;
    private List<String> deliveryProof;
    private String rejectReason;
    private Long lockExpireAt;
    private UserInfo publisher;
    private UserInfo acceptor;
    private BigDecimal estimatedFee;
    private BigDecimal estimatedIncome;
    private Long createdAt;
    private Long updatedAt;

    /**
     * 当前协商轮次对应的接单记录ID（用于聊天室隔离不同轮次）
     * 仅在允许查看聊天室时返回。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long grabRecordId;

    @Data
    public static class UserInfo {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String nickname;
        private String avatar;
        private Integer creditScore;
        private Integer level;
        private String levelName;
        private Double feeDiscount;
    }
}
