package com.campus.task.module.message.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.task.common.exception.BusinessException;
import com.campus.task.common.result.R;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.message.entity.Message;
import com.campus.task.module.message.mapper.MessageMapper;
import com.campus.task.module.message.vo.MessageVO;
import com.campus.task.module.task.entity.Task;
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
import java.util.List;
import java.util.Map;

@Tag(name = "消息模块")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final SnowflakeUtil snowflakeUtil;

    @Operation(summary = "消息列表")
    @GetMapping("/list")
    public R<Page<MessageVO>> list(@AuthenticationPrincipal UserDetails user,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) Integer type) {
        Long userId = Long.valueOf(user.getUsername());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .orderByDesc(Message::getCreatedAt);
        if (type != null) wrapper.eq(Message::getType, type);
        
        Page<Message> msgPage = messageMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<MessageVO> result = new Page<>(msgPage.getCurrent(), msgPage.getSize(), msgPage.getTotal());
        
        List<MessageVO> voList = msgPage.getRecords().stream().map(msg -> {
            MessageVO vo = new MessageVO();
            vo.setId(msg.getId());
            vo.setSenderId(msg.getSenderId());
            vo.setType(msg.getType());
            vo.setContent(msg.getContent());
            vo.setIsRead(msg.getIsRead() == 1);
            vo.setCreatedAt(msg.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
            
            // 填充发送者信息
            if (msg.getSenderId() != 0) {
                User sender = userMapper.selectById(msg.getSenderId());
                if (sender != null) {
                    vo.setSenderNickname(sender.getNickname());
                    vo.setSenderAvatar(sender.getAvatar());
                } else {
                    vo.setSenderNickname("未知用户");
                }
            } else {
                vo.setSenderNickname("系统消息");
            }
            
            // 填充任务标题
            if (msg.getTaskId() != null) {
                Task task = taskMapper.selectById(msg.getTaskId());
                if (task != null) {
                    vo.setTaskId(msg.getTaskId());
                    vo.setTaskTitle(task.getTitle());
                }
            }
            
            return vo;
        }).toList();
        
        result.setRecords(voList);
        return R.ok(result);
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/read/{id}")
    public R<Void> readOne(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails user) {
        Message msg = messageMapper.selectById(id);
        if (msg == null || !Long.valueOf(user.getUsername()).equals(msg.getReceiverId()))
            throw new BusinessException("消息不存在");
        msg.setIsRead(1);
        messageMapper.updateById(msg);
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<Void> readAll(@AuthenticationPrincipal UserDetails user) {
        messageMapper.readAll(Long.valueOf(user.getUsername()));
        return R.ok();
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails user) {
        Message msg = messageMapper.selectById(id);
        if (msg == null || !Long.valueOf(user.getUsername()).equals(msg.getReceiverId()))
            throw new BusinessException("消息不存在");
        messageMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "发送私信")
    @PostMapping("/send")
    public R<Object> send(@AuthenticationPrincipal UserDetails user,
                         @RequestBody Map<String, Object> body) {
        Long senderId = Long.valueOf(user.getUsername());
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = body.get("content").toString();
        if (content.isEmpty() || content.length() > 500)
            throw new BusinessException("消息内容1-500字");
        Message msg = new Message();
        msg.setId(snowflakeUtil.nextId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setType(1);
        msg.setContent(content);
        msg.setIsRead(0);
        if (body.containsKey("taskId") && body.get("taskId") != null)
            msg.setTaskId(Long.valueOf(body.get("taskId").toString()));
        messageMapper.insert(msg);
        return R.ok(Map.of("messageId", String.valueOf(msg.getId())));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public R<Object> unreadCount(@AuthenticationPrincipal UserDetails user) {
        int count = messageMapper.countUnread(Long.valueOf(user.getUsername()));
        return R.ok(Map.of("count", count));
    }
}
