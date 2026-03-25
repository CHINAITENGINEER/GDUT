package com.campus.task.config;

import com.campus.task.module.chat.handler.ChatWebSocketHandler;
import com.campus.task.module.rally.handler.RallyLobbyWebSocketHandler;
import com.campus.task.module.rally.handler.RallyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final RallyLobbyWebSocketHandler rallyLobbyWebSocketHandler;
    private final RallyWebSocketHandler rallyWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat/{taskId}")
                .setAllowedOriginPatterns("*");

        registry.addHandler(rallyLobbyWebSocketHandler, "/ws/rally/lobby")
                .setAllowedOriginPatterns("*");

        registry.addHandler(rallyWebSocketHandler, "/ws/rally/{rallyId}")
                .setAllowedOriginPatterns("*");
    }
}
