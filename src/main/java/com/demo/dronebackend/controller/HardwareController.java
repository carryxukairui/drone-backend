package com.demo.dronebackend.controller;

import com.demo.dronebackend.factory.DeviceReportParserFactory;
import com.demo.dronebackend.factory.DroneReportParserFactory;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HardwareController {
    private final AlarmService alarmService;
    private final DeviceService deviceService;
    private final DroneReportParserFactory droneReportParserFactory;
    private final DeviceReportParserFactory deviceReportParserFactory;
    private final ObjectMapper mapper;

    /**
     * 响应硬件发送请求
     *
     * @param jsonNode 无人机原始侦测上报数据
     */
    @PostMapping("sys/portable/drone/report")
    public Result<?> reportDrone(@RequestBody JsonNode jsonNode) {
        try {
            List<AlarmConvertible> reports = droneReportParserFactory.parse(jsonNode);
            if (reports == null || reports.isEmpty()) return Result.error("无有效数据: " + jsonNode.toString());
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
    @PostMapping("sys/portable/status/report")
    public Result<?> reportStatus(@RequestBody JsonNode jsonNode) {
        try {
            List<DeviceConvertible> reports = deviceReportParserFactory.parse(jsonNode);
            if (reports == null || reports.isEmpty()) return Result.error("无有效数据: " + jsonNode.toString());
            reports.forEach(deviceService::handleDeviceReport);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("解析失败: " + e.getMessage());
        }
    }


    @EventListener
    public void onDeviceReport(DeviceReportEvent e) {
        try {
            JsonNode jsonNode = mapper.readTree(e.getPayload());
            List<DeviceConvertible> reports = deviceReportParserFactory.parse(jsonNode);
            if (reports == null || reports.isEmpty()) return;
            reports.forEach(deviceService::handleDeviceReport);
        } catch (Exception ex) {
            System.out.println("解析失败: " + ex.getMessage());
        }
    }


    @EventListener
    public void onDroneReport(DroneReportEvent e) {
        try {
            JsonNode jsonNode = mapper.readTree(e.getPayload());
            List<AlarmConvertible> reports = droneReportParserFactory.parse(jsonNode);
            if (reports == null || reports.isEmpty()) return;
            reports.forEach(alarmService::handleDroneReport);
        } catch (Exception ex) {
            System.out.println("解析失败: " + ex.getMessage());
        }
    }

}
