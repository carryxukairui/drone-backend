package com.demo.dronebackend.ws;
import com.demo.dronebackend.ws.WebSocketService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class MyWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketService webSocketService;

    public MyWebSocketHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        webSocketService.addSession(session);
        System.out.println("Connection established: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketService.removeSession(session);
        System.out.println("Connection closed: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 可以处理前端发来的消息（如需要双向通信）
        System.out.println("Received from client: " + message.getPayload());
    }
}