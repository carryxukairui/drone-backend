package com.demo.dronebackend.controller;

import com.demo.dronebackend.dto.hardware.Scanner;
import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.DeviceDTO;
import com.demo.dronebackend.dto.screen.DeviceListDTO;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.ws.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final DeviceMapper deviceMapper;
    private DeviceStatusReportController(WebSocketService webSocketService, DeviceMapper deviceMapper) {
        this.webSocketService = webSocketService;
        this.deviceMapper = deviceMapper;
    }

    @PostMapping("/report")
    public Map<String, Object> reportStatus(@RequestBody StatusReport report) {

        System.out.println("Received device status: " + report);
        // 1. 收集所有上报里的设备ID
        List<String> deviceIds = report.getScannerD()
                .stream()
                .map(Scanner::getId)
                .toList();

        List<Device> devices = deviceMapper.selectBatchIds(deviceIds);

        Map<String, List<DeviceDTO>> dtoByUser = new HashMap<>();
        for (Device dev : devices) {
            String userId = String.valueOf(dev.getDeviceUserId());

            DeviceDTO dto = new DeviceDTO();
            dto.setDeviceId(dev.getId());
            dto.setDeviceName(dev.getDeviceName());
            dto.setCoverRange(dev.getCoverRange());
            dto.setPower(dev.getPower());
            dto.setLinkStatus(dev.getLinkStatus());
            //TODO：接入api获取位置信息
            dto.setLocation("详细地址");
            // 加入到对应用户的列表
            dtoByUser.computeIfAbsent(userId, k -> new ArrayList<>())
                    .add(dto);
        }
        // 4. 分用户推送：为每个用户构造子报告并发送
        dtoByUser.forEach((userId, listOfDto) -> {
            DeviceListDTO payload = new DeviceListDTO();
            payload.setDevices(listOfDto);
            webSocketService.sendDeviceListToUser(userId, payload);
        });


        // 3. 返回响应
        return Map.of("code", 200, "msg", "Success");
    }


}