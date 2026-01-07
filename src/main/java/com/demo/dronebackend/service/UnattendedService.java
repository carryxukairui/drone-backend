package com.demo.dronebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dronebackend.dto.device.DeviceCommand;
import com.demo.dronebackend.mapper.*;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.model.TimingWheelDelayManager;
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
    private final TiandituService tiandituService;
    private final TimingWheelDelayManager timingWheelDelayManager;
    private static final String TYPE_LEGAL = "legal";
    // 设备类型常量
    private static final String DEVICE_TYPE_JAMMER = "JAMMER";
    // 常量定义
    private static final String TYPE_ILLEGAL = "illegal";
    public static final String ACTION_ON = "ON";
    public static final String ACTION_OFF = "OFF";
    private static final int INTERFERENCE_DURATION_SECONDS = 10;

    // 干扰频段常量
    private static final int BAND_1_2GHZ = 9;
    private static final int BAND_1_9GHZ = 16;
    private static final int BAND_2_7GHZ = 24;
    private static final int BAND_5_2GHZ = 52;
    private static final int BAND_5_8GHZ = 58;
    private static final double duration = 20.0;

    public void onTdoaAlarm(Alarm alarm, User u) {

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
        // 4. 确定干扰频段  int band = determineJammerBand(alarm.getFrequency());
        //修改  除了1.6G频段外 其余都打开
        int band = 16;


        // 发送 ON 干扰指令（重试机制）
        sendJammerCommandWithRetry(device.getId(), ACTION_ON, band, u, 2);

        // 标记已处置
        alarm.setIsDisposed(1);
        alarmMapper.updateById(alarm);
        logSystemEvent(u, OP_TYPE_UNATTENDED_EVENT,
                String.format("启动干扰 | 设备:%s | 频段:%d | SN:%s",
                        device.getId(), band, alarm.getDroneSn()));

        // 安排自动关闭任务：10秒后检查
        scheduleAutoOff(device.getId(), alarm, band, u);
    }

    public Result<?> disposeAlarmManually(Alarm alarm, User user) {
        if (alarm.getLastLatitude() == null || alarm.getLastLongitude() == null){
            return Result.error("告警经纬度丢失");
        }
        // 查找最近的干扰设备
        Device device = findNearestJammer(alarm, user);
        if (device == null) {
            return Result.error("未找到可用干扰设备");
        }
        // 确定干扰频段\int band = determineJammerBand(alarm.getFrequency());
        //除了1.6G频段外都打开
        int band = 16;
        // 发送 ON 干扰指令（重试机制）
        sendJammerCommandWithRetry(device.getId(), ACTION_ON, band, user, 2);
        // 标记已处置
        alarm.setIsDisposed(1);
        alarmMapper.updateById(alarm);
        // 安排自动关闭任务：10秒后检查
        scheduleAutoOff(device.getId(), alarm, band, user);
        return Result.success("处置成功");
    }

    private void sendJammerCommandWithRetry(String deviceId, String action,
                                            int band, User user, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            if (sendJammerCommand(deviceId, action, band, user,duration)) {
                return; // 成功后退出
            }
        }
        logSystemEvent(user, OP_TYPE_UNATTENDED_EVENT,
                String.format("%s 重试失败 | 设备ID:%s | 频段:%d", action, deviceId, band));
    }

    private void scheduleAutoOff(String deviceId, Alarm alarm, int band, User user) {
        String key = deviceId + ":" + alarm.getDroneSn();
        String droneSn = alarm.getDroneSn();
        timingWheelDelayManager.scheduleTask(key, INTERFERENCE_DURATION_SECONDS, TimeUnit.SECONDS, () -> {
            // 10秒后检查
            Instant cutoff = Instant.now().minusSeconds(INTERFERENCE_DURATION_SECONDS);
            boolean hasRecent = hasRecentAlarms(user.getId(), droneSn, cutoff, alarm.getIntrusionStartTime());

            if (!hasRecent) {
                // 若无新告警，关闭干扰并日志
                sendJammerCommandWithRetry(deviceId, ACTION_OFF, band, user, 2);
                logSystemEvent(user, OP_TYPE_UNATTENDED_EVENT,
                        String.format("自动关闭干扰 | 设备ID:%s | 频段:%d | 无人机SN:%s",
                                deviceId, band, droneSn));
            } else {
                // 若有新告警，重设定时器继续干扰
                logSystemEvent(user, OP_TYPE_UNATTENDED_EVENT,
                        String.format("无人机仍在活动，保持干扰 | 设备ID:%s | 无人机SN:%s",
                                deviceId, droneSn));
                scheduleAutoOff(deviceId, alarm, band, user);
            }
        }, true);
    }

    /**
     * 验证触发条件
     */
    private boolean isValidTrigger(Alarm alarm, User user) {
        // 无人值守情况下只自动处置黑名单无人机，非黑飞无人机不处理
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
        // 判断是否有位置信息
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
                            "经度：" + alarm.getLastLongitude() + " | 纬度：" + alarm.getLastLatitude()
                            //tiandituService.reverseGeocode(alarm.getLastLongitude(), alarm.getLastLatitude())
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
     * 查找距离告警点最近且在覆盖范围内的干扰设备
     */
    private Device findNearestJammer(Alarm alarm, User user) {
        double alarmLat = alarm.getLastLatitude();
        double alarmLon = alarm.getLastLongitude();

        List<Device> candidates = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceUserId, user.getId())
                        .eq(Device::getDeviceType, DEVICE_TYPE_JAMMER)
                        .eq(Device::getLinkStatus, 1)
                        .isNotNull(Device::getLatitude)
                        .isNotNull(Device::getLongitude)
        );


        return candidates.stream()
                // 过滤：距离必须 <= 该设备的 cover_range（单位：米）
                .filter(d -> {
                    double dist = distance(d.getLatitude(), d.getLongitude(), alarmLat, alarmLon);
                    return dist <= (d.getCoverRange() != null ? d.getCoverRange() : 0);
                })
                // 选最小距离
                .min(Comparator.comparing(d ->
                        distance(d.getLatitude(), d.getLongitude(), alarmLat, alarmLon)
                ))
                .orElse(null);
    }

    private List<Region> fetchRegions(long userId, List<Integer> types) {
        return regionMapper.selectByUserAndTypes(userId, types);
    }


    /**
     * 检查近期告警
     */
    private boolean hasRecentAlarms(Long userId, String droneSn, Instant since, Date intrusionStartTime) {
        List<Alarm> alarms = alarmMapper.selectRecentAlarms(droneSn, Date.from(since), intrusionStartTime, userId);
        log.info("近期告警数量:{}", alarms.size());
        // 判断除当前告警外是否还有同一无人机的其他告警
        return alarms.size() > 1;
    }

    /**
     * 发送干扰指令
     */
    public boolean sendJammerCommand(String deviceId, String action, int band, User user,Double dduration) {
        int onoff09 = 1;
        int onoff16 = 1;
        int onoff24 = 1;
        int onoff52 = 1;
        int onoff58 = 1;

        // 再对目标频段做开/关设置
        int mode = 0 ;
//        if ("ON".equalsIgnoreCase(action)) {
//            mode = 1;
//        } else if ("OFF".equalsIgnoreCase(action)) {
//            mode = 0;
//        } else {
//            mode = 2;
//        }

        switch (band) {
            case 9 -> onoff09 = mode;
            case 16 -> onoff16 = mode;
            case 24 -> onoff24 = mode;
            case 52 -> onoff52 = mode;
            case 58 -> onoff58 = mode;
            default -> {
                // 非法频段，可以记录日志或抛异常
                log.warn("未知频段 {}，保持所有频段原状态", band);
            }
        }
        DeviceCommand command = new DeviceCommand(deviceId, onoff09, onoff16, onoff24,onoff52, onoff58,dduration);
        try {
            String payload = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(command);

            String topic = "device/jammer/command/startJam";

            mqttService.publish(topic, payload);
            log.info("MQTT指令已发送 | 主题:{} | 动作:{} | 频段:{}", topic, action, band);
            // 成功也写入日志，方便审计每次下发内容
            logSystemEvent(
                    user,
                    OP_TYPE_UNATTENDED_MQTT_SUCCESS,
                    String.format("MQTT指令已发送 | 设备:%s | 动作:%s | 频段:%d | payload:%s",
                            deviceId, action, band, payload)
            );
            return true;
        } catch (Exception e) {
            String errorMsg = String.format("MQTT指令发送失败 | 设备:%s | 动作:%s | 频段:%d | 错误:%s",
                    deviceId, action, band, e.getMessage());
            log.error(errorMsg, e);
            logSystemEvent(user, OP_TYPE_UNATTENDED_MQTT_FAIL, errorMsg);
            return false;
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
        } else  if(frequency < 5.2){
            return BAND_5_2GHZ;
        }else
            return BAND_5_8GHZ;
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


    public void logSystemEvent(User user, String operationType, String description) {
        SystemLog systemLog = new SystemLog();
        systemLog.setUserId(user.getId());
        systemLog.setUsername(user.getName());
        systemLog.setOperationType(operationType);
        systemLog.setDescription(description);
        //systemLog.setCreatedTime(new Date());

        systemLogMapper.insert(systemLog);
    }
}
