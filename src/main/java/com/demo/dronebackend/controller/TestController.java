package com.demo.dronebackend.controller;

import com.demo.dronebackend.service.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mqtt")
public class TestController {
    private final MqttService mqttService;

    public TestController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @GetMapping("/send")
    public String send(@RequestParam String msg) throws MqttException {
        mqttService.publish("device/status", msg);
        return "发送成功: " + msg;
    }
}
