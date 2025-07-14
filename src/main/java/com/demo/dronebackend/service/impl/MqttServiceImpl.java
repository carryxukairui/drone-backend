package com.demo.dronebackend.service.impl;

import com.demo.dronebackend.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MqttServiceImpl implements MqttService {

    private final IMqttClient mqttClient;

    @Override
    public void publish(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        int qos = 1;
        mqttMessage.setQos(qos);
        mqttClient.publish(topic, mqttMessage);
    }

    @Override
    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException  {
        mqttClient.subscribe(topic, listener);
    }
}
