package com.campus.task.common.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum MessageType {
    SYSTEM(0, "系统消息"),
    PRIVATE(1, "私信");

    private final int code;
    private final String desc;

    MessageType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
