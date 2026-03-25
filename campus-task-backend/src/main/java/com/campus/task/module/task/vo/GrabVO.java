package com.campus.task.module.task.vo;

import lombok.Data;

/**
 * 抢单返回VO
 */
@Data
public class GrabVO {
    /** 锁单过期时间戳（毫秒），前端展示倒计时 */
    private Long lockExpireAt;
    private String message;
    /** 接单记录ID，用于建立聊天室（隔离不同接单轮次） */
    private String grabRecordId;
}
