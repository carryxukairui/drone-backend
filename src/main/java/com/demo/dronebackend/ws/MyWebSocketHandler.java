package com.demo.dronebackend.ws;
import com.demo.dronebackend.ws.WebSocketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;


/*
主要负责在 WebSocket 连接生命周期中的几个关键时刻做一些自定义处理，
并把连接的管理逻辑委托给 WebSocketService
 */
public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final String PREF_ATTR = "USER_PREF";
    private final WebSocketService webSocketService;

    public MyWebSocketHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 1. 从握手时存入的 attributes 中拿到 userId
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            // 2. 按用户添加到 service
            // 初始化默认偏好：不过滤、page=1、size=10
            session.getAttributes().put(PREF_ATTR, new WebSocketService.UserPref("TDOA", 1, 10));
            webSocketService.addSession(userId, session);
            System.out.println("Connection established for user " + userId + ": " + session.getId());
        } else {
            // 没拿到 userId，直接断开（可选）
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No userId in session"));
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            webSocketService.removeSession(userId, session);
            System.out.println("Connection closed for user " + userId + ": " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode json = new ObjectMapper().readTree(message.getPayload());

        WebSocketService.UserPref pref =
                (WebSocketService.UserPref) session.getAttributes().get(PREF_ATTR);
        if (pref == null) {
            pref = new WebSocketService.UserPref(null, 1, 10);
        }

        // 前端只需发送 { deviceType, page, size } 中任意字段
        if (json.has("deviceType")) {
            pref.setDeviceType(json.get("deviceType").asText());
        }
        if (json.has("page")) {
            pref.setPage(json.get("page").asInt());
        }
        if (json.has("size")) {
            pref.setSize(json.get("size").asInt());
        }
        // 写回 session attributes（其实对象是同一个）
        session.getAttributes().put(PREF_ATTR, pref);

        // 更新偏好后立即推一次
        webSocketService.pushToSession(session);
    }
}