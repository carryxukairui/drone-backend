package com.demo.dronebackend.util;

import com.demo.dronebackend.model.DeviceDisposal;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.Device;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceDisposalManager {

    // todo 替换成redis
    private final Map<String, DeviceDisposal> deviceDisposalMap = new ConcurrentHashMap<>();

    // 地球半径（米）
    private final double R = 6_371_000;

    public void startDisposal(Device device, Instant start, Instant end) {
        deviceDisposalMap.put(device.getId(), new DeviceDisposal(start, end, device));
    }

    public String isAlarmInDisposingArea(Alarm alarm) {
        Instant now = Instant.now();
        double alarmLat = alarm.getLastLatitude();
        double alarmLon = alarm.getLastLongitude();
        return deviceDisposalMap.values().stream()
                .filter(disposal -> !now.isBefore(disposal.getStart()) && !now.isAfter(disposal.getEnd()))
                .filter(disposal -> {
                    Device device = disposal.getDevice();
                    if (device == null || device.getLatitude() == null || device.getLongitude() == null) return false;
                    double dist = distance(device.getLatitude(), device.getLongitude(), alarmLat, alarmLon);
                    Double range = device.getCoverRange();
                    return dist <= (range != null ? range : 0);
                })
                .min(Comparator.comparingDouble(disposal -> {
                    Device device = disposal.getDevice();
                    return distance(device.getLatitude(), device.getLongitude(), alarmLat, alarmLon);
                }))
                .map(disposal -> disposal.getDevice().getId())
                .orElse(null);
    }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
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

    public void endDisposal(String deviceId) {
        deviceDisposalMap.remove(deviceId);
    }

}
