package com.campus.task.module.rally.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rally_message")
public class RallyMessage {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long rallyId;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;
}
