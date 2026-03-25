package com.campus.task.module.rally.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.task.common.utils.JwtUtil;
import com.campus.task.common.utils.SnowflakeUtil;
import com.campus.task.module.rally.entity.RallyActivity;
import com.campus.task.module.rally.entity.RallyMember;
import com.campus.task.module.rally.entity.RallyMessage;
import com.campus.task.module.rally.mapper.RallyActivityMapper;
import com.campus.task.module.rally.mapper.RallyMemberMapper;
import com.campus.task.module.rally.mapper.RallyMessageMapper;
import com.campus.task.module.user.entity.User;
import com.campus.task.module.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class RallyWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final RallyActivityMapper rallyActivityMapper;
    private final RallyMemberMapper rallyMemberMapper;
    private final RallyMessageMapper rallyMessageMapper;
    private final UserMapper userMapper;
    private final SnowflakeUtil snowflakeUtil;
    private final ObjectMapper objectMapper;

    private final Map<Long, CopyOnWriteArrayList<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUser = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionRally = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri() != null ? session.getUri().getQuery() : null);
        if (!StringUtils.hasText(token)) {
            closeWithError(session, "缺少token");
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            closeWithError(session, "token无效");
            return;
        }
        String redisToken = redisTemplate.opsForValue().get("token:" + userId);
        if (!token.equals(redisToken)) {
            closeWithError(session, "token已失效");
            return;
        }

        Long rallyId;
        try {
            String path = session.getUri().getPath();
            rallyId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (Exception e) {
            closeWithError(session, "活动ID格式错误");
            return;
        }

        RallyActivity activity = rallyActivityMapper.selectById(rallyId);
        if (activity == null || activity.getStatus() != 0) {
            closeWithError(session, "活动不存在或已结束");
            return;
        }

        RallyMember member = rallyMemberMapper.selectOne(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getUserId, userId)
                .eq(RallyMember::getStatus, 1)
                .last("LIMIT 1"));
        if (member == null) {
            closeWithError(session, "未加入活动，无法进入聊天室");
            return;
        }

        sessionUser.put(session.getId(), userId);
        sessionRally.put(session.getId(), rallyId);
        rooms.computeIfAbsent(rallyId, k -> new CopyOnWriteArrayList<>()).add(session);

        User user = userMapper.selectById(userId);
        sendToSession(session, Map.of(
                "type", "joined",
                "rallyId", String.valueOf(rallyId),
                "userId", String.valueOf(userId),
                "nickname", user != null ? user.getNickname() : "用户"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = sessionUser.get(session.getId());
        Long rallyId = sessionRally.get(session.getId());
        if (userId == null || rallyId == null) return;

        RallyActivity activity = rallyActivityMapper.selectById(rallyId);
        if (activity == null || activity.getStatus() != 0) {
            sendToSession(session, Map.of("type", "error", "msg", "活动已结束，无法发送消息"));
            return;
        }

        RallyMember member = rallyMemberMapper.selectOne(new LambdaQueryWrapper<RallyMember>()
                .eq(RallyMember::getRallyId, rallyId)
                .eq(RallyMember::getUserId, userId)
                .eq(RallyMember::getStatus, 1)
                .last("LIMIT 1"));
        if (member == null) {
            sendToSession(session, Map.of("type", "error", "msg", "您已退出该活动"));
            return;
        }

        String payload = message.getPayload().trim();
        if (!StringUtils.hasText(payload) || payload.length() > 1000) {
            sendToSession(session, Map.of("type", "error", "msg", "消息内容1-1000字"));
            return;
        }

        String content;
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            content = map.get("content") != null ? map.get("content").toString() : payload;
        } catch (Exception e) {
            content = payload;
        }
        if (!StringUtils.hasText(content) || content.length() > 1000) {
            sendToSession(session, Map.of("type", "error", "msg", "消息内容1-1000字"));
            return;
        }

        RallyMessage rallyMessage = new RallyMessage();
        rallyMessage.setId(snowflakeUtil.nextId());
        rallyMessage.setRallyId(rallyId);
        rallyMessage.setSenderId(userId);
        rallyMessage.setContent(content);
        rallyMessageMapper.insert(rallyMessage);

        User sender = userMapper.selectById(userId);
        long ts = rallyMessage.getCreatedAt() != null
                ? rallyMessage.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("type", "message");
        data.put("id", String.valueOf(rallyMessage.getId()));
        data.put("rallyId", String.valueOf(rallyId));
        data.put("senderId", String.valueOf(userId));
        data.put("senderNickname", sender != null ? sender.getNickname() : "用户");
        data.put("senderAvatar", sender != null && sender.getAvatar() != null ? sender.getAvatar() : "");
        data.put("content", content);
        data.put("createdAt", ts);
        broadcastToRoom(rallyId, data);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long rallyId = sessionRally.remove(session.getId());
        sessionUser.remove(session.getId());
        if (rallyId != null) {
            CopyOnWriteArrayList<WebSocketSession> sessions = rooms.get(rallyId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    rooms.remove(rallyId);
                }
            }
        }
    }

    public void broadcastToRoom(Long rallyId, Map<String, Object> data) {
        CopyOnWriteArrayList<WebSocketSession> sessions = rooms.get(rallyId);
        if (sessions == null) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (Exception e) {
                    log.warn("发送活动消息失败，session={}", session.getId());
                }
            }
        }
    }

    private void closeWithError(WebSocketSession session, String reason) {
        try {
            sendToSession(session, Map.of("type", "error", "msg", reason));
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception e) {
            log.warn("关闭连接失败", e);
        }
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> data) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            log.warn("发送消息失败，session={}", session.getId());
        }
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
