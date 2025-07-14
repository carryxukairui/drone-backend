package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.DeviceSettingReq;
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
     * 获取飞行历史
     *
     * @param query 查询参数
     * @return
     */
    @PostMapping("/flight/history")
    public Result<?> historyList(@Valid @RequestBody FlightHistoryQuery query) {
        return alarmService.historyList(query);
    }

    /**
     * websocket获取硬件数据
     * @param report
     * @return
     */
    @PostMapping("/report")
    public Map<String, Object> reportStatus(@RequestBody StatusReport report) {

        System.out.println("Received device status: " + report);
        return deviceService.websocketDevice(report);
    }


    /**
     *  获取远程设备详情页
     *
     */
    @GetMapping("/devices/{id}")
    public Result<?> getDeviceDetail(@PathVariable("id") String deviceId) {
        return deviceService.getDeviceDetail(deviceId);
    }

    /**
     * 提交反制参数设置
     */
    @PostMapping("/devices/{id}/param-settings")
    public Result<?> updateDeviceParamSettings(@PathVariable("id") String deviceId, @RequestBody DeviceSettingReq paramSettings) {
        return deviceService.updateDeviceParamSettings(deviceId, paramSettings);
    }

    /**
     * 获取处置列表及分页展示
     */
    @GetMapping("/devices/disposal-records")
    public Result<?> listDisposalRecords(@RequestParam(value = "page",defaultValue = "1") Integer page,
                                         @RequestParam(value = "size",defaultValue = "10") Integer size) {
        return deviceService.listDisposalRecords(page,size);
    }




    @GetMapping("stats/distribution/hour")
    public Result<?> getHourlyDistribution() {
        return alarmService.getHourlyDistribution();
    }

    @GetMapping("stats/distribution/week")
    public Result<?> getWeeklyDistribution() {
        return alarmService.getWeeklyDistribution();
    }

    @GetMapping("stats/distribution/month")
    public Result<?> getMonthlyDistribution() {
        return alarmService.getMonthlyDistribution();
    }

    @GetMapping("stats/distribution/year")
    public Result<?> getYearDistribution() {
        return alarmService.getYearDistribution();
    }

    @GetMapping("stats/distribution/all-drone")
    public Result<?> getAllDroneDistribution() {
        return alarmService.getAllDroneDistribution();
    }
}
