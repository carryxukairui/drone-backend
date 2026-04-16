package com.demo.dronebackend.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dronebackend.mapper.*;
import com.demo.dronebackend.model.TimingWheelDelayManager;
import com.demo.dronebackend.pojo.*;
import com.demo.dronebackend.service.MqttService;
import com.demo.dronebackend.service.UnattendedService;
import com.demo.dronebackend.util.Result;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.demo.dronebackend.constant.SystemLogConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UnattendedService 单元测试类
 * 覆盖 TC-DISPOSAL-001~005 测试用例
 */
@ExtendWith(MockitoExtension.class)
class UnattendedServiceTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DroneMapper droneMapper;

    @Mock
    private RegionMapper regionMapper;

    @Mock
    private AlarmMapper alarmMapper;

    @Mock
    private MqttService mqttService;

    @Mock
    private SystemLogMapper systemLogMapper;

    @Mock
    private TimingWheelDelayManager timingWheelDelayManager;

    @InjectMocks
    private UnattendedService unattendedService;

    // 测试数据
    private User testUser;
    private Alarm testAlarm;
    private Device testJammerDevice;
    private Region testCoreRegion;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("测试用户");
        testUser.setUnattended(1); // 开启无人值守

        // 初始化测试告警（黑飞无人机，有位置信息）
        testAlarm = new Alarm();
        testAlarm.setId(1L);
        testAlarm.setDroneSn("TEST-001");
        testAlarm.setDroneModel("TestDrone");
        testAlarm.setFrequency(2.4);
        testAlarm.setLastLatitude(30.0);
        testAlarm.setLastLongitude(120.0);
        testAlarm.setIsDisposed(0);
        testAlarm.setIntrusionStartTime(new Date());

        // 初始化测试干扰设备
        testJammerDevice = new Device();
        testJammerDevice.setId("JAMMER-001");
        testJammerDevice.setDeviceName("测试干扰设备");
        testJammerDevice.setDeviceType("JAMMER");
        testJammerDevice.setDeviceUserId(1L);
        testJammerDevice.setLinkStatus(1);
        testJammerDevice.setLatitude(30.001);
        testJammerDevice.setLongitude(120.001);
        testJammerDevice.setCoverRange(1000.0); // 覆盖范围1000米

        // 初始化核心区
        testCoreRegion = new Region();
        testCoreRegion.setId(1L);
        testCoreRegion.setUserId(1L);
        testCoreRegion.setType(1); // 核心区
        testCoreRegion.setCenterLat(30.0);
        testCoreRegion.setCenterLon(120.0);
        testCoreRegion.setRadius(1.0); // 1公里半径
    }

    /**
     * TC-DISPOSAL-001: 无人值守开关状态测试
     * 验证系统日志记录功能
     */
    @Test
    @DisplayName("TC-DISPOSAL-001: 无人值守开关状态 - 验证日志记录")
    void testUnattendedSwitchStatus() {
        // Given
        String operationType = OP_TYPE_UNATTENDED_START;
        String description = "开启无人值守模式";

        // When
        unattendedService.logSystemEvent(testUser, operationType, description);

        // Then
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper).insert(logCaptor.capture());

        SystemLog capturedLog = logCaptor.getValue();
        assertEquals(testUser.getId(), capturedLog.getUserId());
        assertEquals(testUser.getName(), capturedLog.getUsername());
        assertEquals(operationType, capturedLog.getOperationType());
        assertEquals(description, capturedLog.getDescription());
    }

    /**
     * TC-DISPOSAL-002: 自动处置调度-正常流程
     * 模拟黑飞无人机告警，在核心区/反制区内，有可用的干扰设备
     */
    @Test
    @DisplayName("TC-DISPOSAL-002: 自动处置调度-正常流程")
    void testAutoDisposalNormalFlow() throws MqttException {
        // Given
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.singletonList(testCoreRegion));
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 验证MQTT指令发送
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttService, atLeastOnce()).publish(topicCaptor.capture(), payloadCaptor.capture());

        // 验证发送到了正确的topic
        assertTrue(topicCaptor.getValue().contains("device/jammer/command/startJam"));

        // 验证payload包含频段信息
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("g09_onoff") || payload.contains("g16_onoff"),
                "Payload should contain band info: " + payload);

        // 验证告警状态更新为已处置
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmMapper).updateById(alarmCaptor.capture());
        assertEquals(1, alarmCaptor.getValue().getIsDisposed());

        // 验证系统日志记录
        verify(systemLogMapper, atLeast(3)).insert(any(SystemLog.class));

        // 验证延时任务调度
        verify(timingWheelDelayManager).scheduleTask(
                eq(testJammerDevice.getId() + ":" + testAlarm.getDroneSn()),
                eq(10L),
                eq(TimeUnit.SECONDS),
                any(Runnable.class),
                eq(true)
        );
    }

    /**
     * TC-DISPOSAL-003: 自动处置-无可用设备
     * 所有设备忙碌或离线，验证记录警告日志，告警保持未处置
     */
    @Test
    @DisplayName("TC-DISPOSAL-003: 自动处置-无可用设备")
    void testAutoDisposalNoAvailableDevice() {
        // Given
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.singletonList(testCoreRegion));
        // 返回空列表，表示没有可用设备
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 验证告警未被处置
        verify(alarmMapper, never()).updateById(any(Alarm.class));

        // 验证系统日志记录了未找到设备的事件
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper, atLeast(2)).insert(logCaptor.capture());

        List<SystemLog> capturedLogs = logCaptor.getAllValues();
        boolean hasNoDeviceLog = capturedLogs.stream()
                .anyMatch(log -> log.getDescription().contains("未找到可用干扰设备"));
        assertTrue(hasNoDeviceLog, "应记录未找到可用干扰设备的日志");
    }

    /**
     * TC-DISPOSAL-004: 处置超时检测
     * 验证 scheduleAutoOff 方法被调用，即延时任务被调度
     */
    @Test
    @DisplayName("TC-DISPOSAL-004: 处置超时检测 - 验证延时任务调度")
    void testDisposalTimeoutDetection() throws MqttException {
        // Given
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.singletonList(testCoreRegion));
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 验证延时任务被调度（10秒后检查）
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(timingWheelDelayManager).scheduleTask(
                keyCaptor.capture(),
                delayCaptor.capture(),
                timeUnitCaptor.capture(),
                any(Runnable.class),
                anyBoolean()
        );

        assertEquals(testJammerDevice.getId() + ":" + testAlarm.getDroneSn(), keyCaptor.getValue());
        assertEquals(10L, delayCaptor.getValue());
        assertEquals(TimeUnit.SECONDS, timeUnitCaptor.getValue());
    }

    /**
     * TC-DISPOSAL-005: 处置结果回调处理 - MQTT发送成功场景
     */
    @Test
    @DisplayName("TC-DISPOSAL-005: 处置结果回调处理 - MQTT发送成功")
    void testDisposalCallbackSuccess() throws MqttException {
        // Given
        doNothing().when(mqttService).publish(anyString(), anyString());

        // When
        boolean result = unattendedService.sendJammerCommand(
                testJammerDevice.getId(),
                UnattendedService.ACTION_ON,
                16,
                testUser,
                20.0
        );

        // Then
        assertTrue(result);
        verify(mqttService).publish(anyString(), anyString());

        // 验证成功日志记录
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper).insert(logCaptor.capture());
        assertEquals(OP_TYPE_UNATTENDED_MQTT_SUCCESS, logCaptor.getValue().getOperationType());
    }

    /**
     * TC-DISPOSAL-005: 处置结果回调处理 - MQTT发送失败场景
     */
    @Test
    @DisplayName("TC-DISPOSAL-005: 处置结果回调处理 - MQTT发送失败")
    void testDisposalCallbackFailure() throws MqttException {
        // Given
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED))
                .when(mqttService).publish(anyString(), anyString());

        // When
        boolean result = unattendedService.sendJammerCommand(
                testJammerDevice.getId(),
                UnattendedService.ACTION_ON,
                16,
                testUser,
                20.0
        );

        // Then
        assertFalse(result);

        // 验证失败日志记录
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper).insert(logCaptor.capture());
        assertEquals(OP_TYPE_UNATTENDED_MQTT_FAIL, logCaptor.getValue().getOperationType());
    }

    /**
     * 测试非黑飞无人机不处置
     */
    @Test
    @DisplayName("非黑飞无人机不应触发自动处置")
    void testNonIllegalDroneNotDisposed() {
        // Given
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("legal");

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper, never()).updateById(any(Alarm.class));
        verify(deviceMapper, never()).selectList(any());

        // 验证日志记录
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper).insert(logCaptor.capture());
        assertEquals(OP_TYPE_UNATTENDED_NO_DRONE, logCaptor.getValue().getOperationType());
    }

    /**
     * 测试无位置信息的告警不处置
     */
    @Test
    @DisplayName("无位置信息的告警不应触发自动处置")
    void testNoLocationAlarmNotDisposed() {
        // Given
        testAlarm.setLastLatitude(null);
        testAlarm.setLastLongitude(null);
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper, never()).updateById(any(Alarm.class));
        verify(deviceMapper, never()).selectList(any());
    }

    /**
     * 测试区域外告警不处置
     */
    @Test
    @DisplayName("区域外告警不应触发自动处置")
    void testOutOfAreaAlarmNotDisposed() {
        // Given
        testAlarm.setLastLatitude(40.0); // 远离核心区
        testAlarm.setLastLongitude(130.0);
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.singletonList(testCoreRegion));

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper, never()).updateById(any(Alarm.class));
        verify(deviceMapper, never()).selectList(any());

        // 验证区域外日志
        ArgumentCaptor<SystemLog> logCaptor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogMapper, atLeast(1)).insert(logCaptor.capture());

        List<SystemLog> capturedLogs = logCaptor.getAllValues();
        boolean hasOutAreaLog = capturedLogs.stream()
                .anyMatch(log -> log.getOperationType().equals(OP_TYPE_UNATTENDED_OUT_AREA));
        assertTrue(hasOutAreaLog, "应记录区域外日志");
    }

    /**
     * 测试手动处置正常流程
     */
    @Test
    @DisplayName("手动处置正常流程")
    void testManualDisposalNormalFlow() throws MqttException {
        // Given
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        Result<?> result = unattendedService.disposeAlarmManually(testAlarm, testUser);

        // Then
        assertEquals(200, result.getCode());
        assertEquals("处置成功", result.getMessage());

        // 验证告警状态更新
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmMapper).updateById(alarmCaptor.capture());
        assertEquals(1, alarmCaptor.getValue().getIsDisposed());

        // 验证MQTT发送
        verify(mqttService, atLeastOnce()).publish(anyString(), anyString());
    }

    /**
     * 测试手动处置无可用设备
     */
    @Test
    @DisplayName("手动处置无可用设备应返回错误")
    void testManualDisposalNoDevice() {
        // Given
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // When
        Result<?> result = unattendedService.disposeAlarmManually(testAlarm, testUser);

        // Then
        assertEquals(400, result.getCode());
        assertEquals("未找到可用干扰设备", result.getMessage());
    }

    /**
     * 测试手动处置无位置信息
     */
    @Test
    @DisplayName("手动处置无位置信息应返回错误")
    void testManualDisposalNoLocation() {
        // Given
        testAlarm.setLastLatitude(null);
        testAlarm.setLastLongitude(null);

        // When
        Result<?> result = unattendedService.disposeAlarmManually(testAlarm, testUser);

        // Then
        assertEquals(400, result.getCode());
        assertEquals("告警经纬度丢失", result.getMessage());
    }

    /**
     * 测试设备不在覆盖范围内
     */
    @Test
    @DisplayName("设备不在覆盖范围内不应被选中")
    void testDeviceOutOfCoverRange() {
        // Given
        testJammerDevice.setCoverRange(10.0); // 只有10米覆盖范围
        testAlarm.setLastLatitude(30.5); // 远离设备位置
        testAlarm.setLastLongitude(120.5);

        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.emptyList()); // 空区域，默认全域触发
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 告警未被处置，因为设备不在覆盖范围内
        verify(alarmMapper, never()).updateById(any(Alarm.class));
    }

    /**
     * 测试多个设备时选择最近的设备
     */
    @Test
    @DisplayName("多个设备时应选择距离最近的设备")
    void testSelectNearestDevice() throws MqttException {
        // Given
        Device farDevice = new Device();
        farDevice.setId("JAMMER-002");
        farDevice.setDeviceName("远距离干扰设备");
        farDevice.setDeviceType("JAMMER");
        farDevice.setDeviceUserId(1L);
        farDevice.setLinkStatus(1);
        farDevice.setLatitude(30.1); // 更远的位置
        farDevice.setLongitude(120.1);
        farDevice.setCoverRange(5000.0);

        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.emptyList());
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(farDevice, testJammerDevice)); // 远设备在前
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 验证使用了最近的设备（testJammerDevice）
        verify(timingWheelDelayManager).scheduleTask(
                eq(testJammerDevice.getId() + ":" + testAlarm.getDroneSn()),
                anyLong(),
                any(),
                any(),
                anyBoolean()
        );
    }

    /**
     * 测试离线设备不被选中
     */
    @Test
    @DisplayName("离线设备不应被选中")
    void testOfflineDeviceNotSelected() {
        // Given - 模拟查询返回空列表（因为离线设备被LambdaQueryWrapper过滤掉了）
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.emptyList());
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList()); // 离线设备被过滤，返回空列表

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper, never()).updateById(any(Alarm.class));
    }

    /**
     * 测试非JAMMER类型设备不被选中
     */
    @Test
    @DisplayName("非JAMMER类型设备不应被选中")
    void testNonJammerDeviceNotSelected() {
        // Given - 模拟查询返回空列表（因为非JAMMER设备被LambdaQueryWrapper过滤掉了）
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.emptyList());
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList()); // 非JAMMER设备被过滤，返回空列表

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper, never()).updateById(any(Alarm.class));
    }

    /**
     * 测试无区域定义时默认全域触发
     */
    @Test
    @DisplayName("无区域定义时默认全域触发")
    void testNoRegionDefaultAllArea() throws MqttException {
        // Given
        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.emptyList()); // 无区域定义
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then - 应该正常处置
        verify(alarmMapper).updateById(any(Alarm.class));
        verify(mqttService, atLeastOnce()).publish(anyString(), anyString());
    }

    /**
     * 测试反制区（type=2）内告警处置
     */
    @Test
    @DisplayName("反制区内告警应触发处置")
    void testDisposalZoneTrigger() throws MqttException {
        // Given
        Region disposalZone = new Region();
        disposalZone.setId(2L);
        disposalZone.setUserId(1L);
        disposalZone.setType(2); // 反制区
        disposalZone.setCenterLat(30.0);
        disposalZone.setCenterLon(120.0);
        disposalZone.setRadius(2.0);

        when(droneMapper.findTypeBySn(testAlarm.getDroneSn())).thenReturn("illegal");
        when(regionMapper.selectByUserAndTypes(testUser.getId(), Arrays.asList(1, 2)))
                .thenReturn(Collections.singletonList(disposalZone));
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testJammerDevice));
        when(alarmMapper.updateById(any(Alarm.class))).thenReturn(1);
        doNothing().when(mqttService).publish(anyString(), anyString());
        doNothing().when(timingWheelDelayManager).scheduleTask(
                anyString(), anyLong(), any(TimeUnit.class), any(Runnable.class), anyBoolean());

        // When
        unattendedService.onTdoaAlarm(testAlarm, testUser);

        // Then
        verify(alarmMapper).updateById(any(Alarm.class));
        verify(mqttService, atLeastOnce()).publish(anyString(), anyString());
    }
}
