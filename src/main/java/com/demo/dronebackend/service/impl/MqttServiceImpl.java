package com.demo.dronebackend.service.impl;

import com.demo.dronebackend.controller.DeviceReportEvent;
import com.demo.dronebackend.factory.DeviceReportParserFactory;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.service.MqttService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MqttServiceImpl implements MqttService {

    private final IMqttClient mqttClient;
    private final static String TOPIC = "device/status";

    private final ApplicationEventPublisher publisher;
    @Override
    public void publish(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        int qos = 1;
        mqttMessage.setQos(qos);
        mqttClient.publish(topic, mqttMessage);
    }

    @Override
    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        mqttClient.subscribe(topic, listener);
    }

    // 比如在某个初始化方法里订阅
    @PostConstruct
    public void initSubscribe() throws MqttException {
        mqttClient.subscribe(TOPIC, (topic, message) -> {
            String payload = new String(message.getPayload());
            publisher.publishEvent(new DeviceReportEvent(this, payload));

        });

//            try {
//
//                String payload = new String(message.getPayload());
//                System.out.println("收到消息: topic=" + topic + ", payload=" + payload);
//                JsonNode jsonNode = new ObjectMapper().readTree(payload);
//
//                // 解析为 DeviceConvertible 列表
//                List<DeviceConvertible> reports = deviceReportParserFactory.parse(jsonNode);
//
//                // 逐个处理
//                reports.forEach(deviceService::handleDeviceReport);
//
//                System.out.println("处理成功, 收到消息: " + payload);
//            } catch (Exception e) {
//                System.err.println("解析失败: " + e.getMessage());
//            }
    }


}
