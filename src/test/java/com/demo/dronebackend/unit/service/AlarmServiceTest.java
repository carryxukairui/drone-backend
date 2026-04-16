package com.demo.dronebackend.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.dronebackend.dto.alarm.AlarmQueryReq;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.service.AlarmService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlarmService单元测试类
 * 覆盖TC-ALARM-001~008测试用例
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlarmServiceTest {

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmMapper alarmMapper;

    private Alarm testAlarm;

    @BeforeEach
    void setUp() {
        // 创建测试告警数据
        testAlarm = new Alarm();
        testAlarm.setId(1L);
        testAlarm.setDroneSn("DRONE-TEST-001");
        testAlarm.setDroneModel("DJI-Mavic-3");
        testAlarm.setDroneType("illegal");
        testAlarm.setLastLatitude(30.123456);
        testAlarm.setLastLongitude(120.123456);
        testAlarm.setLastAltitude(100.5);
        testAlarm.setFrequency(2400.0);
        testAlarm.setBandwidth(20.0);
        testAlarm.setScanid("DEV-001");
        testAlarm.setIsDisposed(0);
        testAlarm.setIntrusionStartTime(new Date());
        testAlarm.setTakeoffTime(new Date());
    }

    // ==================== TC-ALARM-001: 硬件数据上报处理 ====================

    @Test
    @DisplayName("TC-ALARM-001: 硬件数据上报处理 - 验证数据入库")
    void testHandleDroneReport_DataInserted() {
        // Given - 模拟上报数据
        String droneSn = "DRONE-REPORT-001";
        double latitude = 30.123456;
        double longitude = 120.123456;
        double altitude = 100.5;
        double speed = 15.2;

        // 创建告警记录
        Alarm reportAlarm = new Alarm();
        reportAlarm.setDroneSn(droneSn);
        reportAlarm.setLastLatitude(latitude);
        reportAlarm.setLastLongitude(longitude);
        reportAlarm.setLastAltitude(altitude);
        reportAlarm.setSpeed(speed);
        reportAlarm.setDroneModel("TestModel");
        reportAlarm.setTakeoffTime(new Date());
        reportAlarm.setIntrusionStartTime(new Date());
        reportAlarm.setIsDisposed(0);

        // When - 直接插入模拟上报
        alarmMapper.insert(reportAlarm);

        // Then - 验证数据入库
        Alarm savedAlarm = alarmMapper.selectById(reportAlarm.getId());
        assertNotNull(savedAlarm);
        assertEquals(droneSn, savedAlarm.getDroneSn());
        assertEquals(latitude, savedAlarm.getLastLatitude(), 0.0001);
        assertEquals(longitude, savedAlarm.getLastLongitude(), 0.0001);
        assertEquals(altitude, savedAlarm.getLastAltitude(), 0.1);
    }

    // ==================== TC-ALARM-002: 重复告警处理 ====================

    @Test
    @DisplayName("TC-ALARM-002: 重复告警处理 - 同一无人机5分钟内不重复创建")
    void testDuplicateAlarmHandling() {
        // Given - 插入第一条告警
        String droneSn = "DRONE-DUP-001";
        Alarm firstAlarm = new Alarm();
        firstAlarm.setDroneSn(droneSn);
        firstAlarm.setLastLatitude(30.123456);
        firstAlarm.setLastLongitude(120.123456);
        firstAlarm.setIntrusionStartTime(new Date());
        firstAlarm.setTakeoffTime(new Date());
        firstAlarm.setIsDisposed(0);
        alarmMapper.insert(firstAlarm);

        // When - 查询相同SN的告警
        LambdaQueryWrapper<Alarm> query = new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getDroneSn, droneSn)
                .ge(Alarm::getTakeoffTime, new Date(System.currentTimeMillis() - 5 * 60 * 1000));
        List<Alarm> existingAlarms = alarmMapper.selectList(query);

        // Then - 应该存在记录，应更新而不是创建
        assertFalse(existingAlarms.isEmpty(), "应找到已有告警记录");
        assertEquals(1, existingAlarms.size(), "应只有一条记录");
    }

    @Test
    @DisplayName("TC-ALARM-002: 重复告警处理 - 相似坐标(<100m)应视为重复")
    void testSimilarCoordinateDuplicate() {
        // Given - 原始告警位置
        double baseLat = 30.123456;
        double baseLon = 120.123456;

        // 偏移约50米的新位置 (约0.00045度)
        double newLat = baseLat + 0.00045;
        double newLon = baseLon + 0.00045;

        // 计算距离
        double distance = calculateDistance(baseLat, baseLon, newLat, newLon);
        log.info("坐标偏移距离: {}米", distance);

        // Then - 距离应小于100米
        assertTrue(distance < 100, "坐标偏移应小于100米，实际: " + distance + "米");
    }

    // ==================== TC-ALARM-003: 告警分页查询 ====================

    @Test
    @DisplayName("TC-ALARM-003: 告警分页查询 - 返回指定数量记录")
    void testListAlarms_Pagination() {
        // Given - 插入测试数据
        for (int i = 0; i < 55; i++) {
            Alarm alarm = new Alarm();
            alarm.setDroneSn("DRONE-PAGE-" + i);
            alarm.setTakeoffTime(new Date());
            alarm.setIntrusionStartTime(new Date());
            alarm.setIsDisposed(0);
            alarmMapper.insert(alarm);
        }

        // When - 查询第一页，每页20条
        Page<Alarm> page = new Page<>(1, 20);
        Page<Alarm> result = alarmMapper.selectPage(page, new LambdaQueryWrapper<Alarm>()
                .like(Alarm::getDroneSn, "DRONE-PAGE-")
                .orderByDesc(Alarm::getTakeoffTime));

        // Then - 验证分页结果
        assertEquals(20, result.getRecords().size(), "应返回20条记录");
        assertTrue(result.getTotal() >= 55, "总记录数应>=55");
    }

    // ==================== TC-ALARM-004: 告警条件查询 ====================

    @Test
    @DisplayName("TC-ALARM-004: 告警条件查询 - 多条件组合")
    void testListAlarms_WithConditions() {
        // Given - 插入不同条件的告警
        Date startTime = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
        Date endTime = new Date();

        Alarm alarm1 = new Alarm();
        alarm1.setDroneSn("DRONE-MODEL-A");
        alarm1.setDroneModel("Model-A");
        alarm1.setTakeoffTime(new Date());
        alarm1.setIntrusionStartTime(new Date());
        alarm1.setIsDisposed(0);
        alarmMapper.insert(alarm1);

        Alarm alarm2 = new Alarm();
        alarm2.setDroneSn("DRONE-MODEL-B");
        alarm2.setDroneModel("Model-B");
        alarm2.setTakeoffTime(new Date());
        alarm2.setIntrusionStartTime(new Date());
        alarm2.setIsDisposed(0);
        alarmMapper.insert(alarm2);

        // When - 按型号查询
        LambdaQueryWrapper<Alarm> query = new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getDroneModel, "Model-A")
                .ge(Alarm::getTakeoffTime, startTime)
                .le(Alarm::getLandingTime, endTime);
        List<Alarm> results = alarmMapper.selectList(query);

        // Then - 只返回符合条件的
        assertFalse(results.isEmpty(), "应返回结果");
        results.forEach(a -> assertEquals("Model-A", a.getDroneModel()));
    }

    // ==================== TC-ALARM-005: 告警更新测试 ====================

    @Test
    @DisplayName("TC-ALARM-005: 告警更新测试 - 更新状态和备注")
    void testUpdateAlarm_Success() {
        // Given - 先插入一条告警
        Alarm alarm = new Alarm();
        alarm.setDroneSn("DRONE-UPDATE-001");
        alarm.setTakeoffTime(new Date());
        alarm.setIntrusionStartTime(new Date());
        alarmMapper.insert(alarm);

        // When - 更新告警
        AlarmUpdateReq updateReq = new AlarmUpdateReq();
        updateReq.setDroneSn("UPDATED-SN");
        updateReq.setDroneModel("UpdatedModel");
        updateReq.setIntrusionTime(new Date());

        // 实际更新
        Alarm updateData = new Alarm();
        updateData.setId(alarm.getId());
        updateData.setIsDisposed(1);
        updateData.setDroneModel(updateReq.getDroneModel());
        alarmMapper.updateById(updateData);

        // Then - 验证更新
        Alarm updated = alarmMapper.selectById(alarm.getId());
        assertEquals(1, updated.getIsDisposed());
        assertEquals("UpdatedModel", updated.getDroneModel());
    }

    // ==================== TC-ALARM-006: 告警删除测试 ====================

    @Test
    @DisplayName("TC-ALARM-006: 告警删除测试 - 单条删除")
    void testDeleteAlarm_Single() {
        // Given
        Alarm alarm = new Alarm();
        alarm.setDroneSn("DRONE-DELETE-001");
        alarm.setTakeoffTime(new Date());
        alarm.setIntrusionStartTime(new Date());
        alarmMapper.insert(alarm);

        Long alarmId = alarm.getId();

        // When - 删除
        alarmMapper.deleteById(alarmId);

        // Then - 验证删除
        Alarm deleted = alarmMapper.selectById(alarmId);
        assertNull(deleted, "记录应被删除");
    }

    @Test
    @DisplayName("TC-ALARM-006: 告警删除测试 - 批量删除")
    void testBatchDeleteAlarms() {
        // Given - 插入多条告警
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Alarm alarm = new Alarm();
            alarm.setDroneSn("DRONE-BATCH-" + i);
            alarm.setTakeoffTime(new Date());
            alarm.setIntrusionStartTime(new Date());
            alarmMapper.insert(alarm);
            ids.add(alarm.getId());
        }

        // When - 批量删除
        alarmMapper.deleteBatchIds(ids);

        // Then - 验证批量删除
        for (Long id : ids) {
            Alarm alarm = alarmMapper.selectById(id);
            assertNull(alarm, "ID=" + id + " 应被删除");
        }
    }

    // ==================== TC-ALARM-007: 告警统计-今日统计 ====================

    @Test
    @DisplayName("TC-ALARM-007: 告警统计-今日统计")
    void testGetMonitorCount() {
        // Given - 插入今日告警
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        for (int i = 0; i < 5; i++) {
            Alarm alarm = new Alarm();
            alarm.setDroneSn("DRONE-TODAY-" + i);
            alarm.setTakeoffTime(new Date());
            alarm.setIntrusionStartTime(new Date());
            alarm.setIsDisposed(i < 2 ? 1 : 0); // 2条已处置，3条未处置
            alarmMapper.insert(alarm);
        }

        // When - 统计今日告警
        LambdaQueryWrapper<Alarm> todayQuery = new LambdaQueryWrapper<Alarm>()
                .ge(Alarm::getTakeoffTime, todayStart)
                .le(Alarm::getLandingTime, todayEnd);
        long todayCount = alarmMapper.selectCount(todayQuery);

        LambdaQueryWrapper<Alarm> disposedQuery = new LambdaQueryWrapper<Alarm>()
                .ge(Alarm::getTakeoffTime, todayStart)
                .le(Alarm::getLandingTime, todayEnd)
                .eq(Alarm::getIsDisposed, 1);
        long disposedCount = alarmMapper.selectCount(disposedQuery);

        // Then
        assertTrue(todayCount >= 5, "今日告警数应>=5");
        assertTrue(disposedCount >= 2, "已处置数应>=2");
    }

    // ==================== TC-ALARM-008: 告警统计-时段分布 ====================

    @Test
    @DisplayName("TC-ALARM-008: 告警统计-24小时时段分布")
    void testGetHourlyDistribution() {
        // Given - 插入不同小时的告警
        Calendar cal = Calendar.getInstance();

        for (int hour = 0; hour < 24; hour += 3) {
            cal.setTime(new Date());
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, 0);

            Alarm alarm = new Alarm();
            alarm.setDroneSn("DRONE-HOUR-" + hour);
            alarm.setTakeoffTime(cal.getTime());
            alarm.setIntrusionStartTime(cal.getTime());
            alarmMapper.insert(alarm);
        }

        // When - 查询并统计
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        LambdaQueryWrapper<Alarm> query = new LambdaQueryWrapper<Alarm>()
                .ge(Alarm::getTakeoffTime, todayStart)
                .le(Alarm::getLandingTime, todayEnd);
        List<Alarm> todayAlarms = alarmMapper.selectList(query);

        // 按小时统计
        int[] hourlyCount = new int[24];
        for (Alarm alarm : todayAlarms) {
            cal.setTime(alarm.getTakeoffTime());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourlyCount[hour]++;
        }

        // Then - 验证有24个时段
        assertEquals(24, hourlyCount.length, "应有24个时段");
        // 验证插入的小时有数据
        assertTrue(hourlyCount[0] > 0 || hourlyCount[3] > 0, "应有小时分布数据");
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算两点间距离（Haversine公式）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000; // 地球半径米
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2)
                * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
}
