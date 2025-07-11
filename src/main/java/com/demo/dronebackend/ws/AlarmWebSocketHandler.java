package com.demo.dronebackend.ws;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketService webSocketService;

    public AlarmWebSocketHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String token = getTokenFromSession(session);
            //StpUtil.checkLogin();
            long userId = Long.parseLong(StpUtil.getLoginIdByToken(token).toString());
            userSessionMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
            session.getAttributes().put("userId", userId);
            log.info("WebSocket连接成功：userId={}, session={}", userId, session.getId());
        } catch (Exception e) {
            log.warn("WebSocket鉴权失败，关闭连接");
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj != null) {
            Long userId = (Long) userIdObj;
            List<WebSocketSession> sessions = userSessionMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessionMap.remove(userId);
                }
            }
            log.info("WebSocket断开：userId={}, session={}", userId, session.getId());
        }
    }

    public void pushToUser(Long userId, Object alarmData) {
        List<WebSocketSession> sessions = userSessionMap.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(alarmData);
        } catch (JsonProcessingException e) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    sessions.remove(session);
                    e.printStackTrace();
                }
            }
        }
    }

    private String getTokenFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery(); // ?token=xxx
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return null;
    }
}
