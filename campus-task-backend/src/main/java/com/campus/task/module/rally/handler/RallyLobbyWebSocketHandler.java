package com.campus.task.module.rally.handler;

import com.campus.task.common.utils.JwtUtil;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RallyLobbyWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void broadcast(Map<String, Object> data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(msg);
                } catch (Exception e) {
                    log.warn("发送大厅消息失败，session={}", session.getId());
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
