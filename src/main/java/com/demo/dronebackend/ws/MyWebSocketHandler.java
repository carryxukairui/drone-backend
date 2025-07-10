package com.demo.dronebackend.ws;
import com.demo.dronebackend.ws.WebSocketService;
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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 可以处理客户端发来的消息
        System.out.println("Received from client ("
                + session.getAttributes().get("userId") + "): "
                + message.getPayload());
    }
}