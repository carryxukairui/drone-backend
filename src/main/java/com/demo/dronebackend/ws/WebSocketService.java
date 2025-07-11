package com.demo.dronebackend.ws;

import com.demo.dronebackend.dto.screen.DeviceListDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WebSocketService {

    // key: userId，value: 该用户的所有 WebSocketSession
    private final ConcurrentMap<String, CopyOnWriteArrayList<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    /**
     * 添加某个用户的新会话
     */
    public void addSession(String userId, WebSocketSession session) {
        sessionsByUser
                .computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    /**
     * 移除用户的某个会话
     */
    public void removeSession(String userId, WebSocketSession session) {
        List<WebSocketSession> list = sessionsByUser.get(userId);
        if (list != null) {
            list.remove(session);
            if (list.isEmpty()) {
                sessionsByUser.remove(userId);
            }
        }
    }

    /**
     * 给指定用户推送设备状态（替代原来的全局广播）
     */
    public void sendDeviceListToUser(String userId, DeviceListDTO report) {
        String json = convertToJson(report);
        List<WebSocketSession> list = sessionsByUser.get(userId);
        if (list == null) return;

        System.out.println("Sending to user " + userId + ": " + json);
        for (WebSocketSession session : list) {
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

    /**
     * JSON 序列化
     */
    private String convertToJson(DeviceListDTO report) {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        try {
            return mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON conversion failed\"}";
        }
    }
}
