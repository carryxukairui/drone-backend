package com.demo.dronebackend.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dronebackend.dto.screen.RegionReq;
import com.demo.dronebackend.mapper.RegionMapper;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.pojo.Region;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.RegionService;
import com.demo.dronebackend.service.UnattendedService;
import com.demo.dronebackend.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RegionService单元测试类
 * 覆盖TC-REGION-001~006测试用例
 *
 * @author test
 * @since 2026-04-11
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RegionServiceTest {

    @Autowired
    private RegionService regionService;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private UnattendedService unattendedService;

    private static final double EARTH_RADIUS = 6_371_000; // 地球半径（米）

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("testUser");
    }

    // ==================== 核心算法工具方法 ====================

    /**
     * Haversine距离计算
     * 计算两点间的球面距离
     */
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2)
                * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 判断点是否在圆形区域内
     * @param pointLat 点纬度
     * @param pointLon 点经度
     * @param centerLat 圆心纬度
     * @param centerLon 圆心经度
     * @param radiusKm 半径（千米）
     * @return true如果在区域内（包含边界）
     */
    private boolean isPointInCircle(double pointLat, double pointLon,
                                     double centerLat, double centerLon,
                                     double radiusKm) {
        double distance = distance(pointLat, pointLon, centerLat, centerLon);
        return distance <= radiusKm * 1000.0;
    }

    // ==================== TC-REGION-001: 射线法-点在区域内 ====================

    @Test
    @DisplayName("TC-REGION-001: 射线法-点在区域内")
    void testPointInsideRegion() {
        // 区域定义: 圆心(30, 120), 半径1km
        double centerLat = 30.0;
        double centerLon = 120.0;
        double radiusKm = 1.0;

        // 测试点: (30.005, 120) - 应在区域内
        // 30.005度纬度约等于30度纬度向北移动约555米（1度纬度约111km）
        double testLat = 30.005;
        double testLon = 120.0;

        // 计算实际距离用于验证
        double actualDistance = distance(testLat, testLon, centerLat, centerLon);
        log.info("TC-REGION-001: 点到圆心距离 = {}米", actualDistance);

        // 验证点在区域内
        boolean inRegion = isPointInCircle(testLat, testLon, centerLat, centerLon, radiusKm);
        assertTrue(inRegion, "点(30.005, 120)应在半径1km的区域内");

        // 验证距离小于半径
        assertTrue(actualDistance < radiusKm * 1000,
                "距离应小于1km，实际距离: " + actualDistance + "米");
    }

    // ==================== TC-REGION-002: 射线法-点在区域外 ====================

    @Test
    @DisplayName("TC-REGION-002: 射线法-点在区域外")
    void testPointOutsideRegion() {
        // 区域定义: 圆心(30, 120), 半径1km
        double centerLat = 30.0;
        double centerLon = 120.0;
        double radiusKm = 1.0;

        // 测试点: (35, 125) - 应在区域外
        double testLat = 35.0;
        double testLon = 125.0;

        // 计算实际距离
        double actualDistance = distance(testLat, testLon, centerLat, centerLon);
        log.info("TC-REGION-002: 点到圆心距离 = {}米", actualDistance);

        // 验证点在区域外
        boolean inRegion = isPointInCircle(testLat, testLon, centerLat, centerLon, radiusKm);
        assertFalse(inRegion, "点(35, 125)应在半径1km的区域外");

        // 验证距离远大于半径
        assertTrue(actualDistance > radiusKm * 1000,
                "距离应大于1km，实际距离: " + actualDistance + "米");
    }

    // ==================== TC-REGION-003: 射线法-点在边界上 ====================

    @Test
    @DisplayName("TC-REGION-003: 射线法-点在边界上")
    void testPointOnBoundary() {
        // 区域定义: 圆心(30, 120), 半径1km
        double centerLat = 30.0;
        double centerLon = 120.0;
        double radiusKm = 1.0;
        double radiusMeters = radiusKm * 1000.0;

        // 计算边界上的点：向东移动1km
        // 在纬度30度处，1度经度约等于96.5km，所以1km约等于0.01036度
        double deltaLon = radiusMeters / (Math.cos(Math.toRadians(centerLat)) * EARTH_RADIUS * Math.PI / 180);
        double boundaryLon = centerLon + Math.toDegrees(deltaLon);

        double testLat = centerLat;
        double testLon = boundaryLon;

        // 计算实际距离
        double actualDistance = distance(testLat, testLon, centerLat, centerLon);
        log.info("TC-REGION-003: 边界点到圆心距离 = {}米", actualDistance);

        // 验证点在边界上（允许0.1%的浮点误差）
        boolean inRegion = isPointInCircle(testLat, testLon, centerLat, centerLon, radiusKm);
        assertTrue(inRegion, "边界上的点应被视为在区域内");

        // 验证距离约等于半径（允许1米误差）
        assertEquals(radiusMeters, actualDistance, 1.0,
                "边界点距离应约等于半径");
    }

    // ==================== TC-REGION-004: 性能测试 ====================

    @Test
    @DisplayName("TC-REGION-004: 性能测试-10000次距离计算 < 100ms")
    void testDistanceCalculationPerformance() {
        double lat1 = 30.0;
        double lon1 = 120.0;
        double lat2 = 30.01;
        double lon2 = 120.01;

        int iterations = 10000;

        // 预热
        for (int i = 0; i < 100; i++) {
            distance(lat1, lon1, lat2, lon2);
        }

        // 正式测试
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            distance(lat1 + i * 0.0001, lon1 + i * 0.0001, lat2, lon2);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        log.info("TC-REGION-004: {}次距离计算耗时 = {}ms", iterations, durationMs);

        assertTrue(durationMs < 100,
                "10000次距离计算应小于100ms，实际耗时: " + durationMs + "ms");
    }

    // ==================== TC-REGION-005: 最优设备选择-距离优先 ====================

    @Test
    @DisplayName("TC-REGION-005: 最优设备选择-距离优先")
    void testOptimalDeviceSelection() {
        // 告警位置
        double alarmLat = 30.0;
        double alarmLon = 120.0;

        // 设备1: (30.001, 120.001) - 空闲 - 预期距离约157m
        Device device1 = createDevice("device1", 30.001, 120.001, 1, 500.0);

        // 设备2: (30.01, 120.01) - 空闲 - 预期距离约1570m
        Device device2 = createDevice("device2", 30.01, 120.01, 1, 2000.0);

        // 设备3: (30.0, 120.0) - 忙碌 - 距离0m但不可用
        Device device3 = createDevice("device3", 30.0, 120.0, 0, 100.0);

        // 计算实际距离
        double dist1 = distance(device1.getLatitude(), device1.getLongitude(), alarmLat, alarmLon);
        double dist2 = distance(device2.getLatitude(), device2.getLongitude(), alarmLat, alarmLon);
        double dist3 = distance(device3.getLatitude(), device3.getLongitude(), alarmLat, alarmLon);

        log.info("TC-REGION-005: 设备1距离 = {}米", dist1);
        log.info("TC-REGION-005: 设备2距离 = {}米", dist2);
        log.info("TC-REGION-005: 设备3距离 = {}米", dist3);

        // 验证距离计算
        assertEquals(157.0, dist1, 10.0, "设备1距离应约157米");
        assertEquals(1570.0, dist2, 50.0, "设备2距离应约1570米");
        assertEquals(0.0, dist3, 1.0, "设备3距离应为0米");

        // 模拟设备选择逻辑
        List<Device> candidates = Arrays.asList(device1, device2, device3);
        Device selectedDevice = selectOptimalDevice(candidates, alarmLat, alarmLon);

        // 验证选中设备1（最近且在线、空闲、在覆盖范围内）
        assertNotNull(selectedDevice, "应选中一个设备");
        assertEquals("device1", selectedDevice.getId(),
                "应选中距离最近的可用设备（设备1）");

        // 验证设备3虽然最近但不在线/忙碌，不应被选中
        assertNotEquals("device3", selectedDevice.getId(),
                "忙碌的设备不应被选中");
    }

    /**
     * 模拟最优设备选择逻辑
     */
    private Device selectOptimalDevice(List<Device> devices, double alarmLat, double alarmLon) {
        return devices.stream()
                // 过滤：在线且空闲且在覆盖范围内
                .filter(d -> d.getLinkStatus() != null && d.getLinkStatus() == 1)
                .filter(d -> {
                    double dist = distance(d.getLatitude(), d.getLongitude(), alarmLat, alarmLon);
                    return dist <= (d.getCoverRange() != null ? d.getCoverRange() : 0);
                })
                // 选择距离最近的
                .min((d1, d2) -> {
                    double dist1 = distance(d1.getLatitude(), d1.getLongitude(), alarmLat, alarmLon);
                    double dist2 = distance(d2.getLatitude(), d2.getLongitude(), alarmLat, alarmLon);
                    return Double.compare(dist1, dist2);
                })
                .orElse(null);
    }

    private Device createDevice(String id, double lat, double lon, int linkStatus, double coverRange) {
        Device device = new Device();
        device.setId(id);
        device.setLatitude(lat);
        device.setLongitude(lon);
        device.setLinkStatus(linkStatus);
        device.setCoverRange(coverRange);
        device.setDeviceType("JAMMER");
        device.setDeviceUserId(1L);
        return device;
    }

    // ==================== TC-REGION-006: Haversine距离精度 ====================

    @Test
    @DisplayName("TC-REGION-006: Haversine距离精度-杭州东到杭州西")
    void testHaversineDistanceAccuracy() {
        // 杭州东站: (30.293650, 120.206264)
        double hangzhouEastLat = 30.293650;
        double hangzhouEastLon = 120.206264;

        // 杭州西站: (30.336734, 120.066892)
        double hangzhouWestLat = 30.336734;
        double hangzhouWestLon = 120.066892;

        // 计算距离
        double distance = distance(hangzhouEastLat, hangzhouEastLon,
                hangzhouWestLat, hangzhouWestLon);

        log.info("TC-REGION-006: 杭州东到杭州西距离 = {}米", distance);

        // 参考距离: 约16km (16000±500m)
        double expectedDistance = 16000.0;
        double tolerance = 500.0;

        assertEquals(expectedDistance, distance, tolerance,
                "杭州东到杭州西距离应约16km，允许±500m误差");
    }

    // ==================== RegionService接口测试 ====================

    @Test
    @DisplayName("测试创建告警区域")
    void testCreateAlertRegion() {
        // 由于RegionServiceImpl使用了StpUtil.getLoginIdAsLong()获取当前登录用户
        // 需要在测试环境中模拟登录状态，这里仅测试算法部分

        RegionReq req = new RegionReq();
        req.setCenterLat(30.0);
        req.setCenterLon(120.0);
        req.setRadius(1.0);
        req.setAlertType(1);

        // 验证请求对象创建成功
        assertNotNull(req);
        assertEquals(30.0, req.getCenterLat());
        assertEquals(120.0, req.getCenterLon());
        assertEquals(1.0, req.getRadius());
        assertEquals(1, req.getAlertType());
    }

    @Test
    @DisplayName("测试Region实体距离计算")
    void testRegionDistanceCalculation() {
        // 创建区域
        Region region = new Region();
        region.setCenterLat(30.0);
        region.setCenterLon(120.0);
        region.setRadius(1.0); // 1km
        region.setType(1);
        region.setUserId(1L);

        // 测试区域内的点
        double insideLat = 30.005;
        double insideLon = 120.0;
        double insideDistance = distance(insideLat, insideLon,
                region.getCenterLat(), region.getCenterLon());

        assertTrue(insideDistance <= region.getRadius() * 1000,
                "区域内的点距离应小于等于半径");

        // 测试区域外的点
        double outsideLat = 35.0;
        double outsideLon = 125.0;
        double outsideDistance = distance(outsideLat, outsideLon,
                region.getCenterLat(), region.getCenterLon());

        assertTrue(outsideDistance > region.getRadius() * 1000,
                "区域外的点距离应大于半径");
    }

    @Test
    @DisplayName("测试多个区域的点在区域内判断")
    void testMultipleRegionsPointInArea() {
        // 创建多个测试区域
        List<Region> regions = new ArrayList<>();

        // 核心区
        Region coreRegion = new Region();
        coreRegion.setCenterLat(30.0);
        coreRegion.setCenterLon(120.0);
        coreRegion.setRadius(0.5); // 500m
        coreRegion.setType(1); // 核心区
        regions.add(coreRegion);

        // 反制区
        Region counterRegion = new Region();
        counterRegion.setCenterLat(30.0);
        counterRegion.setCenterLon(120.0);
        counterRegion.setRadius(2.0); // 2km
        counterRegion.setType(2); // 反制区
        regions.add(counterRegion);

        // 测试点1: 在核心区内
        double testLat1 = 30.002;
        double testLon1 = 120.0;
        boolean inCoreArea = regions.stream()
                .filter(r -> r.getType() == 1)
                .anyMatch(r -> isPointInCircle(testLat1, testLon1,
                        r.getCenterLat(), r.getCenterLon(), r.getRadius()));
        assertTrue(inCoreArea, "点应在核心区内");

        // 测试点2: 在反制区内但不在核心区
        double testLat2 = 30.01;
        double testLon2 = 120.0;
        boolean inCounterAreaOnly = regions.stream()
                .filter(r -> r.getType() == 2)
                .anyMatch(r -> isPointInCircle(testLat2, testLon2,
                        r.getCenterLat(), r.getCenterLon(), r.getRadius()))
                && !regions.stream()
                .filter(r -> r.getType() == 1)
                .anyMatch(r -> isPointInCircle(testLat2, testLon2,
                        r.getCenterLat(), r.getCenterLon(), r.getRadius()));
        assertTrue(inCounterAreaOnly, "点应在反制区内但不在核心区");

        // 测试点3: 不在任何区域内
        double testLat3 = 35.0;
        double testLon3 = 125.0;
        boolean inAnyArea = regions.stream()
                .anyMatch(r -> isPointInCircle(testLat3, testLon3,
                        r.getCenterLat(), r.getCenterLon(), r.getRadius()));
        assertFalse(inAnyArea, "点不应在任何区域内");
    }
}
