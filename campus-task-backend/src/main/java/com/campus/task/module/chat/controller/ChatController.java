package com.campus.task.module.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.R;
import com.campus.task.module.chat.entity.ChatMessage;
import com.campus.task.module.chat.mapper.ChatMessageMapper;
import com.campus.task.module.task.entity.GrabRecord;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.mapper.GrabRecordMapper;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "协商聊天")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageMapper chatMessageMapper;
    private final GrabRecordMapper grabRecordMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;

    @Operation(summary = "获取接单协商聊天历史（按grabRecordId隔离轮次）")
    @GetMapping("/history/{grabRecordId}")
    public R<List<Map<String, Object>>> history(
            @PathVariable Long grabRecordId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        GrabRecord grabRecord = grabRecordMapper.selectById(grabRecordId);
        if (grabRecord == null) throw new BusinessException("接单记录不存在");
        Task task = taskMapper.selectById(grabRecord.getTaskId());
        if (task == null) throw new BusinessException("任务不存在");
        if (!userId.equals(task.getPublisherId()) && !userId.equals(grabRecord.getUserId())) {
            throw new BusinessException(403, "无权查看此聊天记录");
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getGrabRecordId, grabRecordId)
                        .orderByAsc(ChatMessage::getCreatedAt)
                        .last("LIMIT 200")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            User sender = userMapper.selectById(msg.getSenderId());
            long ts = msg.getCreatedAt() != null
                    ? msg.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli() : 0L;
            Map<String, Object> item = new HashMap<>();
            item.put("id", String.valueOf(msg.getId()));
            item.put("grabRecordId", String.valueOf(grabRecordId));
            item.put("taskId", String.valueOf(msg.getTaskId()));
            item.put("senderId", String.valueOf(msg.getSenderId()));
            item.put("senderNickname", sender != null ? sender.getNickname() : "用户");
            item.put("senderAvatar", sender != null && sender.getAvatar() != null ? sender.getAvatar() : "");
            item.put("content", msg.getContent());
            item.put("createdAt", ts);
            result.add(item);
        }
        return R.ok(result);
    }
}
