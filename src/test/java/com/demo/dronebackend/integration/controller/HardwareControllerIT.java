package com.demo.dronebackend.integration.controller;

import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HardwareController集成测试
 * 覆盖TC-API-HW-001~002测试用例
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class HardwareControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private DeviceService deviceService;

    /**
     * TC-API-HW-001: POST /sys/portable/drone/report
     * 无人机侦测数据上报
     */
    @Test
    @DisplayName("TC-API-HW-001: 无人机侦测数据上报接口")
    void testDroneReport_Success() throws Exception {
        // 准备上报数据
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("droneSn", "DRONE001");
        reportData.put("droneType", "DJI");
        reportData.put("droneModel", "Mavic3");
        reportData.put("lat", 30.123456);
        reportData.put("lng", 120.123456);
        reportData.put("alt", 100.5);
        reportData.put("speed", 15.5);
        reportData.put("pilotLat", 30.124);
        reportData.put("pilotLng", 120.124);

        mockMvc.perform(post("/sys/portable/drone/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("处理成功"));
    }

    /**
     * TC-API-HW-002: POST /sys/portable/status/report
     * 设备状态上报
     */
    @Test
    @DisplayName("TC-API-HW-002: 设备状态上报接口")
    void testDeviceStatusReport_Success() throws Exception {
        // 准备状态上报数据
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("deviceId", "DEV001");
        statusData.put("onlineStatus", 1);
        statusData.put("battery", 85);
        statusData.put("temperature", 42.5);

        mockMvc.perform(post("/sys/portable/status/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("处理成功"));
    }

    /**
     * 测试无人机上报 - 缺少必填字段
     */
    @Test
    @DisplayName("无人机上报 - 缺少必填字段")
    void testDroneReport_MissingFields() throws Exception {
        Map<String, Object> reportData = new HashMap<>();
        // 只放部分字段
        reportData.put("droneSn", "DRONE002");

        mockMvc.perform(post("/sys/portable/drone/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportData)))
                .andExpect(status().isOk()); // 业务层可能还是返回200，内部处理
    }

    /**
     * 测试设备状态上报 - 设备不存在
     */
    @Test
    @DisplayName("设备状态上报 - 设备不存在")
    void testDeviceStatusReport_DeviceNotFound() throws Exception {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("deviceId", "DEV-NOT-EXIST");
        statusData.put("onlineStatus", 1);
        statusData.put("battery", 85);

        mockMvc.perform(post("/sys/portable/status/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 测试无效的JSON格式
     */
    @Test
    @DisplayName("上报接口 - 无效JSON格式")
    void testReport_InvalidJson() throws Exception {
        mockMvc.perform(post("/sys/portable/drone/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
}
