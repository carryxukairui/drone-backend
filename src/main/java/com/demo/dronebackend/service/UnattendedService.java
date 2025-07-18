package com.demo.dronebackend.service;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dronebackend.dto.device.DeviceCommand;
import com.demo.dronebackend.mapper.*;
import com.demo.dronebackend.model.DelayTaskManager;
import com.demo.dronebackend.pojo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.demo.dronebackend.constant.SystemLogConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnattendedService {


    private final DeviceMapper deviceMapper;
    private final DroneMapper droneMapper;
    private final RegionMapper regionMapper;
    private final AlarmMapper alarmMapper;
    private final MqttService mqttService;
    private final SystemLogMapper systemLogMapper;
    private final DelayTaskManager delayTaskManager;
    private final TiandituService tiandituService;
    private static final String TYPE_LEGAL = "legal";
    // 设备类型常量
    private static final String DEVICE_TYPE_JAMMER = "JAMMER";
    // 常量定义
    private static final String TYPE_ILLEGAL = "illegal";
    private static final String ACTION_ON = "ON";
    private static final String ACTION_OFF = "OFF";
    private static final int INTERFERENCE_DURATION_SECONDS = 10;

    // 干扰频段常量
    private static final int BAND_1_2GHZ = 9;
    private static final int BAND_1_9GHZ = 16;
    private static final int BAND_2_7GHZ = 24;
    private static final int BAND_5_8GHZ = 58;

    public void onTdoaAlarm(Alarm alarm, User u, boolean isManualTrigger) {

        // 先判断是否是手动处置，手动则跳过，非手动才会去判断是否是无人值守
        if (!isManualTrigger && u.getUnattended() != 1) {
            // 非手动且有人值守，返回
            return;
        }
        // 检查用户和无人机状态
        if (!isValidTrigger(alarm, u)) return;

        // 区域判断
        if (!isInActionArea(alarm, u)) return;

        // 查找最近的干扰设备
        Device device = findNearestJammer(alarm, u);
        if (device == null) {
            logSystemEvent(
                    u,
                    OP_TYPE_UNATTENDED_EVENT,
                    String.format("检测到黑飞无人机但未找到可用干扰设备 | 无人机SN:%s | 位置:%.6f,%.6f",
                            alarm.getDroneSn(), alarm.getLastLatitude(), alarm.getLastLongitude())
            );
            return;
        }
        // 记录检测事件
        logSystemEvent(
                u,
                OP_TYPE_UNATTENDED_EVENT,
                String.format("检测到黑飞无人机 | 无人机SN:%s | 频率:%.2fMHz | 位置:%.6f,%.6f | 分配设备:%s",
                        alarm.getDroneSn(), alarm.getFrequency(),
                        alarm.getLastLatitude(), alarm.getLastLongitude(),
                        device.getDeviceName())
        );
        // 4. 确定干扰频段
        int band = determineJammerBand(alarm.getFrequency());

        // 5. 发送干扰指令并管理定时任务
        handleJammerOperation(alarm, device, band, u );

    }

    /**
     * 验证触发条件
     */
    private boolean isValidTrigger(Alarm alarm, User user) {
        // 非黑名单无人机
        if (!TYPE_ILLEGAL.equals(droneMapper.findTypeBySn(alarm.getDroneSn()))) {
            logSystemEvent(
                    user,
                    OP_TYPE_UNATTENDED_NO_DRONE,
                    String.format("检测到非黑飞无人机 | 无人机SN:%s | 频率:%.2fMHz | 位置:%.6f,%.6f",
                            alarm.getDroneSn(), alarm.getFrequency(),
                            alarm.getLastLatitude(), alarm.getLastLongitude())
            );
            return false;
        }

        // 缺少必要位置信息
        return alarm.getLastLatitude() != null && alarm.getLastLongitude() != null;
    }


    /**
     * 区域判断逻辑
     */
    private boolean isInActionArea(Alarm alarm, User user) {
        // 1=核心区, 2=反制区
        List<Region> regions = fetchRegions(user.getId(), Arrays.asList(1, 2));

        // 用户未定义任何区域时，默认全域触发
        if (regions.isEmpty()) {
            System.out.println("用户未定义任何区域，默认全域触发");
            return true;
        }

        // 检查是否在任一有效区域内
        boolean inArea = regions.stream().anyMatch(region ->
                region.getCenterLat() != null &&
                        region.getCenterLon() != null &&
                        region.getRadius() != null &&
                        distance(
                                region.getCenterLat(), region.getCenterLon(),
                                alarm.getLastLatitude(), alarm.getLastLongitude()
                        ) <= region.getRadius() * 1000.0
        );

        if (inArea) {
            // 在核心区/反制区内，写“击中”日志
            logSystemEvent(
                    user,
                    OP_TYPE_UNATTENDED_IN_AREA,
                    String.format(
                            "检测到黑飞无人机 | 无人机SN:%s | 频率:%.2fMHz | 位置:%.6f,%.6f | 落入%s",
                            alarm.getDroneSn(),
                            alarm.getFrequency(),
                            alarm.getLastLatitude(),
                            alarm.getLastLongitude(),
                            tiandituService.reverseGeocode(alarm.getLastLongitude(), alarm.getLastLatitude())
                    )
            );
            return true;
        } else {
            // 不在任何定义的区域，写“未命中”日志
            logSystemEvent(
                    user,
                    OP_TYPE_UNATTENDED_OUT_AREA,
                    String.format(
                            "检测到黑飞无人机 | 无人机SN:%s | 频率:%.2fMHz | 位置:%.6f,%.6f | 区域外",
                            alarm.getDroneSn(),
                            alarm.getFrequency(),
                            alarm.getLastLatitude(),
                            alarm.getLastLongitude()
                    )
            );
            return false;
        }
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
    private void handleJammerOperation(Alarm alarm, Device device, int band,User user) {

        // 发送开启指令
        sendJammerCommand(device.getId(), ACTION_ON, band,user);
        // 记录干扰开始事件
        logSystemEvent(
                user,
                OP_TYPE_UNATTENDED_EVENT,
                String.format("启动干扰设备 | 设备ID:%s | 频段:%d | 无人机SN:%s",
                        device.getId(), band, alarm.getDroneSn())
        );

        // 管理定时任务
        manageTimeoutTask(alarm, device, band,user);
    }

    /**
     * 管理超时任务
     */
    private void manageTimeoutTask(Alarm alarm, Device device, int band,User user) {
        String droneSn = alarm.getDroneSn();
        // 创建关闭任务
        Runnable closeTask = () -> {
            log.info("检查干扰状态 | 设备ID:{} | 无人机SN:{}", device.getId(), droneSn);

            // 检查过去10秒内是否有同一无人机的告警
            Instant checkStartTime = Instant.now().minusSeconds(INTERFERENCE_DURATION_SECONDS);
            if (!hasRecentAlarms(droneSn, checkStartTime,alarm.getIntrusionStartTime())) {
                log.info("关闭干扰设备 | 设备ID:{} | 频段:{} | 无人机SN:{}",
                        device.getId(), band, droneSn);
                sendJammerCommand(device.getId(), ACTION_OFF, band,user);
                // 记录干扰结束事件
                logSystemEvent(
                        user,
                        OP_TYPE_UNATTENDED_EVENT,
                        String.format("关闭干扰设备 | 设备ID:%s | 频段:%d | 无人机SN:%s",
                                device.getId(), band, droneSn)
                );
            } else {
                // 无人机仍在活动，重新调度检查
                manageTimeoutTask(alarm, device, band, user);
                logSystemEvent(
                        user,
                        OP_TYPE_UNATTENDED_EVENT,
                        String.format("无人机仍在活动，保持干扰 | 设备ID:%s | 无人机SN:%s",
                                device.getId(), droneSn)
                );
            }
        };

        // 使用DelayTaskManager调度任务
        // 任务标识: 设备ID:无人机SN
        String key = device.getId() + ":" + droneSn;

        delayTaskManager.scheduleDelayTask(key, closeTask, INTERFERENCE_DURATION_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 检查近期告警
     */
    private boolean hasRecentAlarms(String droneSn, Instant since, Date intrusionStartTime) {
        long userId = StpUtil.getLoginIdAsLong();
        List<Alarm> alarms = alarmMapper.selectRecentAlarms(droneSn, Date.from(since), intrusionStartTime, userId);
        // 判断除当前告警外是否还有同一无人机的其他告警
        return alarms.size() > 1;
    }

    /**
     * 发送干扰指令
     */
    private void sendJammerCommand(String deviceId, String action, int band, User user) {
        int onoff09 = 2;
        int onoff16 = 2;
        int onoff24 = 2;
        int onoff58 = 2;

        // 再对目标频段做开/关设置
        int mode;
        if ("ON".equalsIgnoreCase(action)) {
            mode = 1;
        } else if ("OFF".equalsIgnoreCase(action)) {
            mode = 0;
        } else {
            mode = 2;
        }

        switch (band) {
            case 9    -> onoff09 = mode;
            case 16   -> onoff16 = mode;
            case 24   -> onoff24 = mode;
            case 58   -> onoff58 = mode;
            default   -> {
                // 非法频段，可以记录日志或抛异常
                log.warn("未知频段 {}，保持所有频段原状态", band);
            }
        }
        DeviceCommand command = new DeviceCommand(deviceId, onoff09, onoff16, onoff24, onoff58);
        try {
            String payload = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(command);

            String topic = "device/command/startJam";
            String message = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(payload);

            mqttService.publish(topic, message);

            log.info("MQTT指令已发送 | 主题:{} | 动作:{} | 频段:{}", topic, action, band);
            // 成功也写入日志，方便审计每次下发内容
            logSystemEvent(
                    user,
                    OP_TYPE_UNATTENDED_MQTT_SUCCESS,
                    String.format("MQTT指令已发送 | 设备:%s | 动作:%s | 频段:%d | payload:%s",
                            deviceId, action, band, payload)
            );
        } catch (Exception e) {
            String errorMsg = String.format("MQTT指令发送失败 | 设备:%s | 动作:%s | 频段:%d | 错误:%s",
                    deviceId, action, band, e.getMessage());
            log.error(errorMsg, e);
            logSystemEvent(user, OP_TYPE_UNATTENDED_MQTT_FAIL, errorMsg);
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


    private void logSystemEvent(User user, String operationType, String description) {
        SystemLog systemLog = new SystemLog();
        systemLog.setUserId(user.getId());
        systemLog.setUsername(user.getName());
        systemLog.setOperationType(operationType);
        systemLog.setDescription(description);
        systemLog.setCreatedTime(new Date());

        systemLogMapper.insert(systemLog);
    }
}
