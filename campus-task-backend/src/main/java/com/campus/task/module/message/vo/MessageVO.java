package com.campus.task.module.message.vo;

import lombok.Data;

@Data
public class MessageVO {
    private Long id;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Long taskId;
    private String taskTitle;
    private Integer type;
    private String content;
    private Boolean isRead;
    private Long createdAt;
}
