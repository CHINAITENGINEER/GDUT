package com.campus.task.module.chat.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.utils.JwtUtil;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.chat.entity.ChatMessage;
import com.campus.task.module.chat.mapper.ChatMessageMapper;
import com.campus.task.module.task.entity.GrabRecord;
import com.campus.task.module.task.entity.Task;
import com.campus.task.module.task.mapper.GrabRecordMapper;
import com.campus.task.module.task.mapper.TaskMapper;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 协商聊天 WebSocket 处理器
 * 连接URL: ws://host/ws/chat/{grabRecordId}?token=xxx
 * 以 grabRecordId 为聊天室KEY，隔离不同接单轮次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final TaskMapper taskMapper;
    private final GrabRecordMapper grabRecordMapper;
    private final UserMapper userMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SnowflakeUtil snowflakeUtil;
    private final ObjectMapper objectMapper;

    /** grabRecordId -> sessions */
    private final Map<Long, CopyOnWriteArrayList<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    /** sessionId -> userId */
    private final Map<String, Long> sessionUser = new ConcurrentHashMap<>();
    /** sessionId -> grabRecordId */
    private final Map<String, Long> sessionGrab = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 验证 token
        String token = extractToken(session.getUri() != null ? session.getUri().getQuery() : null);
        if (!StringUtils.hasText(token)) { closeWithError(session, "缺少token"); return; }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) { closeWithError(session, "token无效"); return; }
        String redisToken = redisTemplate.opsForValue().get("token:" + userId);
        if (!token.equals(redisToken)) { closeWithError(session, "token已失效"); return; }

        // 2. 解析 grabRecordId from path /ws/chat/{grabRecordId}
        String path = session.getUri().getPath();
        Long grabRecordId;
        try {
            grabRecordId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) { closeWithError(session, "grabRecordId格式错误"); return; }

        // 3. 查询接单记录，校验是否存在且状态为锁单中(0)或已确认(1)
        GrabRecord grabRecord = grabRecordMapper.selectById(grabRecordId);
        if (grabRecord == null) { closeWithError(session, "接单记录不存在"); return; }
        if (grabRecord.getStatus() != 0 && grabRecord.getStatus() != 1) {
            closeWithError(session, "该接单已结束，无法进入聊天室"); return;
        }

        // 4. 校验权限：必须是任务发布者 或 本次接单者
        Task task = taskMapper.selectById(grabRecord.getTaskId());
        if (task == null) { closeWithError(session, "任务不存在"); return; }
        boolean isPublisher = userId.equals(task.getPublisherId());
        boolean isCurrentAcceptor = userId.equals(grabRecord.getUserId());
        if (!isPublisher && !isCurrentAcceptor) {
            closeWithError(session, "无权加入此聊天室"); return;
        }

        // 5. 注册 session
        sessionUser.put(session.getId(), userId);
        sessionGrab.put(session.getId(), grabRecordId);
        rooms.computeIfAbsent(grabRecordId, k -> new CopyOnWriteArrayList<>()).add(session);

        User user = userMapper.selectById(userId);
        log.info("用户[{}]({})加入接单[{}]聊天室", userId, user != null ? user.getNickname() : "", grabRecordId);

        Map<String, Object> joinedMsg = new HashMap<>();
        joinedMsg.put("type", "joined");
        joinedMsg.put("userId", String.valueOf(userId));
        joinedMsg.put("nickname", user != null ? user.getNickname() : "用户");
        sendToSession(session, joinedMsg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = sessionUser.get(session.getId());
        Long grabRecordId = sessionGrab.get(session.getId());
        if (userId == null || grabRecordId == null) return;

        // 再次校验接单记录状态（防止协商超时后仍发消息）
        GrabRecord grabRecord = grabRecordMapper.selectById(grabRecordId);
        if (grabRecord == null || (grabRecord.getStatus() != 0 && grabRecord.getStatus() != 1)) {
            Map<String, Object> err = new HashMap<>();
            err.put("type", "error"); err.put("msg", "协商已结束，无法发送消息");
            sendToSession(session, err); return;
        }

        String payload = message.getPayload().trim();
        if (!StringUtils.hasText(payload) || payload.length() > 1000) {
            Map<String, Object> err = new HashMap<>();
            err.put("type", "error"); err.put("msg", "消息内容1-1000字");
            sendToSession(session, err); return;
        }

        String content;
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            content = map.get("content") != null ? map.get("content").toString() : payload;
        } catch (Exception e) { content = payload; }
        if (!StringUtils.hasText(content)) return;

        // 入库，关联 grabRecordId
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setId(snowflakeUtil.nextId());
        chatMsg.setGrabRecordId(grabRecordId);
        chatMsg.setTaskId(grabRecord.getTaskId());
        chatMsg.setSenderId(userId);
        chatMsg.setContent(content);
        chatMessageMapper.insert(chatMsg);

        User sender = userMapper.selectById(userId);
        long ts = chatMsg.getCreatedAt() != null
                ? chatMsg.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "message");
        broadcast.put("id", String.valueOf(chatMsg.getId()));
        broadcast.put("grabRecordId", String.valueOf(grabRecordId));
        broadcast.put("taskId", String.valueOf(grabRecord.getTaskId()));
        broadcast.put("senderId", String.valueOf(userId));
        broadcast.put("senderNickname", sender != null ? sender.getNickname() : "用户");
        broadcast.put("senderAvatar", sender != null && sender.getAvatar() != null ? sender.getAvatar() : "");
        broadcast.put("content", content);
        broadcast.put("createdAt", ts);
        broadcastToRoom(grabRecordId, broadcast);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long grabRecordId = sessionGrab.remove(session.getId());
        Long userId = sessionUser.remove(session.getId());
        if (grabRecordId != null) {
            CopyOnWriteArrayList<WebSocketSession> sessions = rooms.get(grabRecordId);
            if (sessions != null) { sessions.remove(session); if (sessions.isEmpty()) rooms.remove(grabRecordId); }
        }
        log.info("用户[{}]断开聊天连接，接单[{}]", userId, grabRecordId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误，session={}", session.getId(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    private void broadcastToRoom(Long grabRecordId, Map<String, Object> data) {
        CopyOnWriteArrayList<WebSocketSession> sessions = rooms.get(grabRecordId);
        if (sessions == null) return;
        String json; try { json = objectMapper.writeValueAsString(data); } catch (Exception e) { return; }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) { try { s.sendMessage(msg); } catch (Exception e) { log.warn("发送消息失败 session={}", s.getId()); } }
        }
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> data) {
        try { session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data))); }
        catch (Exception e) { log.warn("发送消息失败 session={}", session.getId()); }
    }

    private void closeWithError(WebSocketSession session, String reason) {
        try {
            Map<String, Object> err = new HashMap<>(); err.put("type", "error"); err.put("msg", reason);
            sendToSession(session, err);
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception e) { log.warn("关闭连接失败", e); }
    }

    private String extractToken(String query) {
        if (!StringUtils.hasText(query)) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
        }
        return null;
    }
}
