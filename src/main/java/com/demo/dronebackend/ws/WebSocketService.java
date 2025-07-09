package com.demo.dronebackend.ws;

import com.demo.dronebackend.controller.DeviceStatusReportController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WebSocketService {
    
    // 存储所有活跃的WebSocket会话
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    // 添加新会话
    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }
    
    // 移除会话
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }
    
    // 广播设备状态给所有客户端
    public void broadcastStatus(DeviceStatusReportController.StatusReport report) {
        String json = convertToJson(report); // 转换为JSON格式
        System.out.println("Broadcasting status: " + json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    System.out.println("Sent to client: " + session.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // 使用Jackson转换对象为JSON
    private String convertToJson(DeviceStatusReportController.StatusReport report) {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        try {
            return mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON conversion failed\"}";
        }
    }
}