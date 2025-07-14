package com.demo.dronebackend.ws;


import com.demo.dronebackend.constant.SystemConstants;
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
        String topicKey  = (String) session.getAttributes().get(SystemConstants.ALARM_WEBSOCKET_TOPIC);
        if (topicKey==null){
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No userId in session"));
            } catch (IOException ignored) {}
        }
        webSocketService.addSession(topicKey, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String topicKey  = (String) session.getAttributes().get(SystemConstants.ALARM_WEBSOCKET_TOPIC);
        if (topicKey != null) {
            webSocketService.removeSession(topicKey, session);
        }
    }

}
