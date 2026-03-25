package com.campus.task.common.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {
    PENDING_AUDIT(0, "待审核"),
    PENDING_GRAB(1, "待接单"),
    GRABBED(2, "已抢单待协商"),
    PENDING_PAYMENT(3, "待支付"),
    IN_PROGRESS(4, "进行中"),
    COMPLETED(5, "已完成"),
    SETTLED(6, "已结算"),
    REVIEWED(7, "已互评"),
    CANCELLED(8, "已取消");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatus of(int code) {
        for (TaskStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知任务状态: " + code);
    }
}
