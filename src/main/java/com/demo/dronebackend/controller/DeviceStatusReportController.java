package com.demo.dronebackend.controller;

import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.ws.WebSocketService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 *
 * 用于接收硬件数据
 */
@RestController
@RequestMapping("/sys/portable/status")
public class DeviceStatusReportController {

    private final WebSocketService webSocketService;

    public DeviceStatusReportController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @PostMapping("/report")
    public Map<String, Object> reportStatus(@RequestBody StatusReport report) {
        // 1. 这里可以添加数据验证和存储逻辑
        System.out.println("Received device status: " + report);
        
        // 2. 通过WebSocket广播给所有前端客户端
        webSocketService.broadcastStatus(report);
        
        // 3. 返回响应
        return Map.of("code", 200, "msg", "Success");
    }


}