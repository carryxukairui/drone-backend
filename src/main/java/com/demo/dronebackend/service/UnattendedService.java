package com.demo.dronebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.mapper.DroneMapper;
import com.demo.dronebackend.mapper.RegionMapper;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.pojo.Region;
import com.demo.dronebackend.pojo.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnattendedService {


    private final DeviceMapper deviceMapper;
    private final DroneMapper droneMapper;
    private final RegionMapper regionMapper;
    private final IMqttClient mqttClient;
    private final AlarmMapper alarmMapper;

    // 记录每次启动反制的定时任务 Future，避免重复
    private final ConcurrentMap<String, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private static final String TYPE_LEGAL = "legal";
    private static final String TYPE_ILLEGAL = "illegal";
    // 设备类型常量
    private static final String DEVICE_TYPE_JAMMER = "JAMMER";
    private static final String DEVICE_TYPE_TDOA = "tdoa";

    // 干扰频段常量
    private static final int BAND_1_2GHZ = 9;
    private static final int BAND_1_9GHZ = 16;
    private static final int BAND_2_7GHZ = 24;
    private static final int BAND_5_8GHZ = 58;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public void onTdoaAlarm(Alarm alarm, User u) {
        System.out.println("接收到TDOA告警");
        // 1. 检查用户和无人机状态
        if (!isValidTrigger(alarm, u)) return;

        // 2. 区域判断
        if (!isInActionArea(alarm, u)) return;

        // 3. 查找最近的干扰设备
        Device device = findNearestJammer(alarm, u);
        if (device == null) return;

        // 4. 确定干扰频段
        int band = determineJammerBand(alarm.getFrequency());

        // 5. 发送干扰指令并管理定时任务
        handleJammerOperation(alarm.getDroneSn(), device, band);
    }

    /**
     * 验证触发条件
     */
    private boolean isValidTrigger(Alarm alarm, User user) {
        // 用户非无人值守模式
        if (user.getUnattended() != 1) return false;

        // 非黑名单无人机
        if (!TYPE_ILLEGAL.equals(droneMapper.findTypeBySn(alarm.getDroneSn()))) {
            return false;
        }

        // 缺少必要位置信息
        return alarm.getLastLatitude() != null && alarm.getLastLongitude() != null;
    }


    /**
     * 区域判断逻辑优化
     */
    private boolean isInActionArea(Alarm alarm, User user) {
        // 1=核心区, 2=反制区
        List<Region> regions = fetchRegions(user.getId(), Arrays.asList(1, 2));

        // 用户未定义任何区域时，默认全域触发
        if (regions.isEmpty()) return true;

        // 检查是否在任一有效区域内
        return regions.stream().anyMatch(region ->
                region.getCenterLat() != null &&
                        region.getCenterLon() != null &&
                        region.getRadius() != null &&
                        distance(region.getCenterLat(), region.getCenterLon(),
                                alarm.getLastLatitude(), alarm.getLastLongitude())
                                <= region.getRadius() * 1000.0
        );
    }


    /**
     * 查找最近的干扰设备
     */
    private Device findNearestJammer(Alarm alarm, User user) {
        List<Device> jammers = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceUserId, user.getId())
                        .eq(Device::getDeviceType, DEVICE_TYPE_JAMMER)
                        .eq(Device::getLinkStatus, 1)
                        .isNotNull(Device::getLatitude)
                        .isNotNull(Device::getLongitude)
        );

        // 添加设备状态检查
        return jammers.stream()
                .min(Comparator.comparing(d ->
                        distance(d.getLatitude(), d.getLongitude(),
                                alarm.getLastLatitude(), alarm.getLastLongitude())
                ))
                .orElse(null);
    }

    private List<Region> fetchRegions(long userId, List<Integer> types) {
        return regionMapper.selectByUserAndTypes(userId, types);
    }

    /**
     * 处理干扰设备操作
     */
    private void handleJammerOperation(String droneSn, Device device, int band) {
        System.out.println("处理干扰设备操作");
        System.out.println("干扰设备ID：" + device.getId());

        // 发送开启指令
        sendJammerCommand(device.getId(), "ON", band);

        // 管理定时任务
        manageTimeoutTask(droneSn, device, band);
    }

    /**
     * 管理超时任务
     */
    private void manageTimeoutTask(String droneSn, Device device, int band) {
        // 取消现有任务
        ScheduledFuture<?> existingTask = timeoutTasks.remove(droneSn);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        // 记录任务启动时间
        final Instant taskStartTime = Instant.now();

        // 创建新任务
        ScheduledFuture<?> newTask = scheduler.schedule(() -> {
            if (!hasRecentAlarms(droneSn, taskStartTime)) {
                sendJammerCommand(device.getId(), "OFF", band);
            }
            timeoutTasks.remove(droneSn);
        }, 10, TimeUnit.SECONDS);

        timeoutTasks.put(droneSn, newTask);
    }

    /**
     * 检查近期告警
     */
    private boolean hasRecentAlarms(String droneSn, Instant since) {
        return alarmMapper.selectCount(new LambdaQueryWrapper<Alarm>()
                .eq(Alarm::getDroneSn, droneSn)
                .ge(Alarm::getIntrusionStartTime, Date.from(since)))
                > 0;
    }

    /**
     * 发送干扰指令
     */
    private void sendJammerCommand(String deviceId, String action, int band) {
        try {
            Map<String, Object> payload = Map.of(
                    "action", action,
                    "band", band,
                    "timestamp", System.currentTimeMillis()
            );

            MqttMessage message = new MqttMessage(new ObjectMapper().writeValueAsBytes(payload));
            // 确保至少送达一次
            message.setQos(1);

            mqttClient.publish("device/" + deviceId + "/control", message);
        } catch (Exception e) {
            log.error("MQTT指令发送失败 | 设备:{} | 动作:{} | 频段:{}",
                    deviceId, action, band, e);
        }
    }

    /**
     * 确定干扰频段
     */
    private int determineJammerBand(double frequency) {
        if (frequency < 1.2) {   // 1.2GHz
            return BAND_1_2GHZ;
        } else if (frequency < 1.9) {  // 1.9GHz
            return BAND_1_9GHZ;
        } else if (frequency < 2.7) {  // 2.7GHz
            return BAND_2_7GHZ;
        } else {
            return BAND_5_8GHZ;  // 5.8GHz
        }
    }
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        // 地球半径（米）
        final double R = 6_371_000;
        // 转成弧度
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        // Haversine 公式
        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2)
                * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 返回距离（米）
        return R * c;
    }
}
