package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.DeviceSettingReq;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.dto.screen.RealtimeAlarmReq;
import com.demo.dronebackend.dto.screen.RegionReq;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.service.RegionService;
import com.demo.dronebackend.service.DisposalRecordService;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final AlarmService alarmService;
    private final DeviceService deviceService;
    private final DisposalRecordService disposalRecordService;

    private final RegionService regionService;
    private final UserService userService;

    /**
     * 响应硬件发送请求
     *
     * @param report 无人机侦测上报数据
     */
    @PostMapping("/report/drone")
    public Result<?> reportDrone(@Valid @RequestBody DroneReport report) {
        return alarmService.handleDroneReport(report);
    }

    /**
     * 1.进入大屏时调用，实时告警界面获取历史告警信息
     * 2.调整req中查询参数时调用
     *
     * @param req 请求体
     */
    @GetMapping("alarms")
    public Result<?> realtimeAlarms(@Valid @ModelAttribute RealtimeAlarmReq req) {
        return alarmService.realtimeAlarms(req);
    }

    @GetMapping("alarms/{id}")
    public Result<?> getAlarm(@NotBlank @PathVariable String id) {
        return alarmService.getAlarm(id);
    }

    @GetMapping("alarms/dispose/{id}")
    public Result<?> disposeDrone(@NotBlank @PathVariable String id) {
        return alarmService.disposeDrone(id);
    }

    /**
     * 获取飞行历史
     *
     * @param query 查询参数
     * @return
     */
    @PostMapping("/flight/history")
    public Result<?> historyList(@RequestBody FlightHistoryQuery query) {
        return alarmService.historyList(query);
    }

    /**
     * websocket获取硬件数据
     *
     * @param report
     * @return
     */
    @PostMapping("/report")
    public Map<String, Object> reportStatus(@RequestBody StatusReport report) {

        System.out.println("Received device status: " + report);

        // 返回响应
        return deviceService.websocketDevice(report);
    }

    /**
     * 首次获取设备列表
     */
    @GetMapping("/devices")
    public Result<?> getDeviceList() {
        return deviceService.getDeviceList();
    }


    /**
     * 获取远程设备详情页
     */
    @GetMapping("/devices/{id}")
    public Result<?> getDeviceDetail(@PathVariable("id") String deviceId) {
        return deviceService.getDeviceDetail(deviceId);
    }

    /**
     * 提交反制参数设置
     */
    @PostMapping("/devices/{id}/param-settings")
    public Result<?> updateDeviceParamSettings(@PathVariable("id") String deviceId, @RequestBody DeviceSettingReq paramSettings) throws MqttException {
        return deviceService.updateDeviceParamSettings(deviceId, paramSettings);
    }

    /**
     * 获取处置列表及分页展示
     */
    @GetMapping("/devices/disposal-records")
    public Result<?> listDisposalRecords(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                         @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return deviceService.listDisposalRecords(page, size);
    }

    /**
     * 时段架次统计
     *
     * @return
     */
    @GetMapping("stats/distribution/hour")
    public Result<?> getHourlyDistribution() {
        return alarmService.getHourlyDistribution();
    }


    /**
     * 周次架次统计
     *
     * @return
     */
    @GetMapping("stats/distribution/week")
    public Result<?> getWeeklyDistribution() {
        return alarmService.getWeeklyDistribution();
    }


    /**
     * 月架次统计
     *
     * @return
     */
    @GetMapping("stats/distribution/month")
    public Result<?> getMonthlyDistribution() {
        return alarmService.getMonthlyDistribution();
    }


    /**
     * 年架次统计
     *
     * @return
     */
    @GetMapping("stats/distribution/year")
    public Result<?> getYearDistribution() {
        return alarmService.getYearDistribution();
    }


    /**
     * 总架次统计
     *
     * @return
     */
    @GetMapping("stats/distribution/all-drone")
    public Result<?> getAllDroneDistribution() {
        return alarmService.getAllDroneDistribution();
    }


    /**
     * 今日动态
     */
    @GetMapping("dynamics/monitor-count")
    public Result<?> getMonitorCount() {
        return alarmService.getMonitorCount();
    }

    @GetMapping("dynamics/disposal-count")
    public Result<?> getDisposalCount() {
        return disposalRecordService.getDisposalCount();
    }

    @GetMapping("dynamics/brand-count")
    public Result<?> getBrandCount() {
        return alarmService.getBrandCount();
    }


    @GetMapping("dynamics/sorties-by-hour")
    public Result<?> getSortiesByHour() {
        return alarmService.getSortiesByHour();
    }

    /**
     * 创建预警区、核心区、反制区
     */
    @PostMapping("/alert/create")
    public Result<?> createAlertRegion(@RequestBody @Valid RegionReq req) {
        return regionService.createAlertRegion(req);
    }

    /**
     * 无人值守
     */
    @PostMapping("/{flag}/unattended")
    public Result<?> setUnattended(@PathVariable("flag") Boolean flag) {
        return userService.setUnattended(flag);
    }


    /**
     * 获取防空区信息
     */
    @GetMapping("/alert")
    public Result<?> getAlertRegion() {
        return regionService.getAlertRegion();
    }

    /**
     * 删除防空区信息
     */
    @DeleteMapping("/alert/{id}")
    public Result<?> deleteAlertRegion(@PathVariable String id) {
        return regionService.deleteAlertRegion(id);
    }
}

