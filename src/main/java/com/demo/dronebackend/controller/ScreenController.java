package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.alarm.RealtimeAlarmReq;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final AlarmService alarmService;

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
     * @param query 查询参数
     * @return
     */
    @PostMapping("/flight/history")
    public Result<?> historyList(@Valid @RequestBody FlightHistoryQuery query){
        return alarmService.historyList(query);
    }
}
