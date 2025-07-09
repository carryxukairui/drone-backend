package com.demo.dronebackend.controller;

import com.demo.dronebackend.ws.WebSocketService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    @Data
    // 设备状态上报数据结构
    public static class StatusReport {
        private String stationId;
        private List<Scanner> scannerD;

        @Data
        public static class Scanner {
            private String id;
            private Integer linkState;
            private Double dataRate;
            private Integer foundTarget;
            private Double lng;
            private Double lat;
            private String ip;
            

        }
    }
}