package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final AlarmService alarmService;

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
