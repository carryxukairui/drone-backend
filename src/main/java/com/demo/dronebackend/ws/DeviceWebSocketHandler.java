package com.demo.dronebackend.ws;

import com.demo.dronebackend.constant.DeviceType;
import com.demo.dronebackend.constant.SystemConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;


/*
主要负责在 WebSocket 连接生命周期中的几个关键时刻做一些自定义处理，
并把连接的管理逻辑委托给 WebSocketService
 */

@Slf4j
public class DeviceWebSocketHandler extends TextWebSocketHandler {
    private static final String PREF_ATTR = "USER_PREF";
    private final WebSocketService webSocketService;

    public DeviceWebSocketHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 1. 从握手时存入的 attributes 中拿到 userId
        String deviceTopicKey  = (String) session.getAttributes().get(SystemConstants.DEVICES_WEBSOCKET_TOPIC);
        String unAttendedTopicKey  = (String) session.getAttributes().get(SystemConstants.UNATTENDED_WEBSOCKET_TOPIC);
        if (deviceTopicKey  != null) {
            // 初始化默认偏好：不过滤、page=1、size=10
            session.getAttributes().put(PREF_ATTR, new WebSocketService.UserPref(DeviceType.TDOA, 1, 10));
            webSocketService.addSession(deviceTopicKey , session);
            log.info("Connection established for user " + deviceTopicKey  + ": " + session.getId());
        } else {
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No userId in session"));
            } catch (IOException ignored) {}
        }

        if (unAttendedTopicKey != null) {
            webSocketService.addSession(unAttendedTopicKey, session);
            log.info("Connection established for 无人值守连接： user " + unAttendedTopicKey  + ": " + session.getId());
        } else {
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("No 无人值守连: userId in session"));
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String deviceTopicKey  = (String) session.getAttributes().get(SystemConstants.DEVICES_WEBSOCKET_TOPIC);
        String unAttendedTopicKey  = (String) session.getAttributes().get(SystemConstants.UNATTENDED_WEBSOCKET_TOPIC);
        if (deviceTopicKey != null) {
            webSocketService.removeSession(deviceTopicKey, session);
            log.info("Connection closed for user " + deviceTopicKey + ": " + session.getId());
        }
        if (unAttendedTopicKey != null){
            webSocketService.removeSession(unAttendedTopicKey, session);
            log.info("Connection closed for 无人值守连接： user " + unAttendedTopicKey + ": " + session.getId());
        }
    }


    //前端传的json信息
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