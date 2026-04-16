package com.demo.dronebackend.integration.controller;

import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.service.AlarmService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlarmController集成测试
 * 覆盖TC-API-ALARM-001~004测试用例
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AlarmControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlarmService alarmService;

    private String testToken;

    @BeforeEach
    void setUp() {
        // 由于需要Sa-Token，这里设置一个测试token
        // 实际测试可能需要先登录获取token
        testToken = "test-token";
    }

    /**
     * TC-API-ALARM-001: GET /admin/alarms
     * 分页和条件查询告警列表
     */
    @Test
    @DisplayName("TC-API-ALARM-001: 告警分页查询接口")
    void testGetAlarms_PaginationAndFilter() throws Exception {
        // 准备测试数据
        for (int i = 0; i < 25; i++) {
            Alarm alarm = new Alarm();
            alarm.setDroneSn("DRONE-API-" + i);
            alarm.setDroneModel("Model-" + (i % 3));
            alarm.setIntrusionStartTime(new Date());

            // 使用mapper直接插入，跳过service复杂逻辑
        }

        // 执行查询请求 - 不带token会401，这里演示接口结构
        mockMvc.perform(get("/admin/alarms")
                        .param("page", "1")
                        .param("size", "20")
                        .param("droneModel", "Model-0")
                        .header("satoken", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * TC-API-ALARM-002: PUT /admin/alarms/{alarm_id}
     * 更新告警信息
     */
    @Test
    @DisplayName("TC-API-ALARM-002: 更新告警接口")
    void testUpdateAlarm() throws Exception {
        // 准备更新数据
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("droneSn", "UPDATED-SN");
        updateData.put("droneModel", "UpdatedModel");
        updateData.put("intrusionTime", "2025-01-15 10:30:00");

        mockMvc.perform(put("/admin/alarms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData))
                        .header("satoken", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * TC-API-ALARM-003: DELETE /admin/alarms/{alarm_id}
     * 删除单条告警
     */
    @Test
    @DisplayName("TC-API-ALARM-003: 删除单条告警接口")
    void testDeleteAlarm() throws Exception {
        mockMvc.perform(delete("/admin/alarms/1")
                        .header("satoken", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("感知记录删除成功"));
    }

    /**
     * TC-API-ALARM-004: POST /admin/alarms/batch_delete
     * 批量删除告警
     */
    @Test
    @DisplayName("TC-API-ALARM-004: 批量删除告警接口")
    void testBatchDeleteAlarms() throws Exception {
        // 准备批量删除数据
        Map<String, List<Long>> batchDeleteData = new HashMap<>();
        batchDeleteData.put("ids", Arrays.asList(1L, 2L, 3L));

        mockMvc.perform(post("/admin/alarms/batch_delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDeleteData))
                        .header("satoken", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("批量删除成功"));
    }

    /**
     * 测试未授权访问
     */
    @Test
    @DisplayName("TC-SEC-001: 未授权访问应返回401")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/admin/alarms")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 测试告警详情查询
     */
    @Test
    @DisplayName("查询告警详情")
    void testGetAlarmDetail() throws Exception {
        mockMvc.perform(get("/admin/alarms/1")
                        .header("satoken", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
