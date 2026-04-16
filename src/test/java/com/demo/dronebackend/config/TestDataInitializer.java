package com.demo.dronebackend.config;

import com.demo.dronebackend.pojo.*;
import com.demo.dronebackend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 测试数据初始化类
 * 用于准备测试所需的测试用户、设备、区域等数据
 */
@Component
public class TestDataInitializer {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private DroneMapper droneMapper;

    @Autowired
    private AlarmMapper alarmMapper;

    /**
     * 初始化测试用户
     */
    public User createTestUser() {
        User user = new User();
        user.setName("测试用户");
        user.setPassword("test_password");
        user.setPhone("13800138000");
        user.setUnattended(1);
        userMapper.insert(user);
        return user;
    }

    /**
     * 初始化测试设备
     */
    public Device createTestDevice() {
        Device device = new Device();
        device.setId("TEST-DEVICE-001");
        device.setDeviceName("测试设备");
        device.setDeviceType("JAMMER");
        device.setDeviceUserId(1L);
        device.setLinkStatus(1);
        device.setLatitude(30.0);
        device.setLongitude(120.0);
        device.setCoverRange(1000.0);
        device.setReportTime(new Date());
        deviceMapper.insert(device);
        return device;
    }

    /**
     * 初始化测试区域
     */
    public Region createTestRegion() {
        Region region = new Region();
        region.setType(1); // 核心区
        region.setUserId(1L);
        region.setCenterLat(30.0);
        region.setCenterLon(120.0);
        region.setRadius(1.0);
        region.setTime(new Date());
        regionMapper.insert(region);
        return region;
    }

    /**
     * 初始化测试无人机
     */
    public Drone createTestDrone() {
        Drone drone = new Drone();
        drone.setDroneSn("TEST-DRONE-001");
        drone.setDroneBrand("TestBrand");
        drone.setDroneModel("TestModel");
        drone.setType("illegal");
        drone.setUserId(1L);
        drone.setUpdateTime(new Date());
        droneMapper.insert(drone);
        return drone;
    }

    /**
     * 初始化测试告警
     */
    public Alarm createTestAlarm() {
        Alarm alarm = new Alarm();
        alarm.setDroneSn("TEST-DRONE-001");
        alarm.setDroneModel("TestModel");
        alarm.setFrequency(2.4);
        alarm.setLastLatitude(30.0);
        alarm.setLastLongitude(120.0);
        alarm.setIsDisposed(0);
        alarm.setIntrusionStartTime(new Date());
        alarmMapper.insert(alarm);
        return alarm;
    }

    /**
     * 清理所有测试数据
     */
    public void cleanAll() {
        // 根据实际业务依赖关系清理数据
        // 注意：由于使用了@Transactional，通常不需要手动清理
    }
}
