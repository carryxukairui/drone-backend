package com.demo.dronebackend.ws;


import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;


public class AlarmWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketService webSocketService;

    public AlarmWebSocketHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId==null){
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No userId in session"));
            } catch (IOException ignored) {}
        }
        webSocketService.addSession(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            webSocketService.removeSession(userId, session);
        }
    }

}
