package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.RealtimeAlarmReq;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final AlarmService alarmService;
    private final DeviceService deviceService;


    /**
     * 实时告警界面获取历史告警信息
     * @param req 请求体
     */
    @GetMapping("alarms")
    public Result<?> realtimeAlarms(@Valid @ModelAttribute RealtimeAlarmReq req){
        return alarmService.realtimeAlarms(req);
    }

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
