package com.demo.dronebackend.service;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;

public interface MqttService {
    void publish(String topic, String message) throws MqttException;
    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException;
}
