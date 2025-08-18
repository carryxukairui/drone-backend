package com.demo.dronebackend.controller;

import com.demo.dronebackend.factory.DeviceReportParserFactory;
import com.demo.dronebackend.factory.DroneReportParserFactory;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Consumer;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HardwareController {
    private final AlarmService alarmService;
    private final DroneReportParserFactory droneReportParserFactory;
    private final DeviceReportParserFactory deviceReportParserFactory;
    private Consumer<DeviceConvertible> handleDeviceReport;

    /**
     * 响应硬件发送请求
     *
     * @param jsonNode 无人机原始侦测上报数据
     */
    @PostMapping("sys/portable/drone/report")
    public Result<?> reportDrone(@RequestBody JsonNode jsonNode) {
        try {
            List<AlarmConvertible> reports = droneReportParserFactory.parse(jsonNode);
            reports.forEach(alarmService::handleDroneReport);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * websocket获取硬件数据
     *
     * @param jsonNode 设备状态原始上报数据
     */
    @PostMapping("admin/devices/sub")
    public Result<?> reportStatus(@RequestBody JsonNode jsonNode) {
        try {
            List<DeviceConvertible> reports = deviceReportParserFactory.parse(jsonNode);
            reports.forEach(handleDeviceReport);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("解析失败: " + e.getMessage());
        }
    }

}
