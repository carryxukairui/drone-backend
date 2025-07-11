package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.ws.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final AlarmService alarmService;
    private final DeviceService deviceService;

    /**
     * 获取飞行历史
     *
     * @param query 查询参数
     * @return
     */
    @PostMapping("/flight/history")
    public Result<?> historyList(@Valid @RequestBody FlightHistoryQuery query) {
        return alarmService.historyList(query);
    }

    @PostMapping("/report")
    public Map<String, Object> reportStatus(@RequestBody StatusReport report) {

        System.out.println("Received device status: " + report);

        // 3. 返回响应
        return deviceService.websocketDevice(report);
    }
}
