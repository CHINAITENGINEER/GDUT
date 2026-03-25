package com.campus.task.module.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 接单记录ID，隔离不同接单轮次的聊天 */
    private Long grabRecordId;
    private Long taskId;
    private Long senderId;
    private String content;
    private String images;
    private LocalDateTime createdAt;
}
