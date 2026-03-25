package com.campus.task.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 发送者ID，0=系统消息 */
    private Long senderId;
    private Long receiverId;
    private Long taskId;
    /** 0=系统消息 1=私信 */
    private Integer type;
    private String content;
    /** 0=未读 1=已读 */
    private Integer isRead;
    private LocalDateTime createdAt;
}
