package com.demo.dronebackend.ws;

import com.demo.dronebackend.dto.screen.DeviceDTO;
import com.demo.dronebackend.dto.screen.RealTimeAlarmDTO;
import com.demo.dronebackend.model.MyPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WebSocketService {

    // key: userId，value: 该用户的所有 WebSocketSession
    private final ConcurrentMap<String, CopyOnWriteArrayList<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<DeviceDTO>> lastDeviceMap = new ConcurrentHashMap<>();


    @Data
    public static class UserPref {
        private String deviceType;
        private int page;
        private int size;
        public UserPref(String deviceType, int page, int size) {
            this.deviceType = deviceType;
            this.page = page;
            this.size = size;
        }

    }
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
    public void sendDeviceListToUser(String userId,  DeviceDTO  allDto) {
        String json = convertToJson(allDto);
        List<WebSocketSession> list = sessionsByUser.get(userId);
        if (list == null) return;

        System.out.println("Sending to user " + userId + ": " + json);
        for (WebSocketSession session : list) {
            if (!session.isOpen()) continue;


            List<DeviceDTO> allDtos = new ArrayList<>();
            allDtos.add(allDto);

            MyPage<DeviceDTO> report = new MyPage<>();
            report.setCurrent(1);
            report.setSize(10);
            report.setTotal(allDtos.size());
            report.setPages(allDtos.size());
            report.setRecords(allDtos);
            report.setSocketType("device");

            // 发送
            try {
                String payload = new ObjectMapper()
                        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .writeValueAsString(report);
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 推送实时告警给用户
     * @param userId 用户id
     * @param myPage 分页告警信息
     */
    public void sendAlarmListToUser(String userId, MyPage<RealTimeAlarmDTO> myPage) {
        List<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            myPage.setSocketType("alarm");
            // JSON 序列化
            String payload = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(myPage);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON 序列化
     */
    private String convertToJson( DeviceDTO report) {
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        try {
            return mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON conversion failed\"}";
        }
    }

    /** 当偏好变更时，前端也可以直接调用： */
    public void pushToSession(WebSocketSession session) {
        // 假设你有一个全局缓存 lastDeviceMap: userId -> List<DeviceDTO>
//        String userId = (String) session.getAttributes().get(SystemConstants.DEVICES_WEBSOCKET_TOPIC);
//        List<DeviceDTO> all = lastDeviceMap.getOrDefault(userId, Collections.emptyList());
//        sendDeviceListToUser(userId, all);
    }
}
