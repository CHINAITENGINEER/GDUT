package com.campus.task.module.message.service;

import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.message.entity.Message;
import com.campus.task.module.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 消息推送服务（供其他模块调用）
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final SnowflakeUtil snowflakeUtil;

    /**
     * 推送系统消息给指定用户
     */
    public void pushSystem(Long receiverId, String content) {
        pushSystem(receiverId, null, content);
    }

    public void pushSystem(Long receiverId, Long taskId, String content) {
        Message msg = new Message();
        msg.setId(snowflakeUtil.nextId());
        msg.setSenderId(0L);
        msg.setReceiverId(receiverId);
        msg.setTaskId(taskId);
        msg.setType(0);
        msg.setContent(content);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }
}
