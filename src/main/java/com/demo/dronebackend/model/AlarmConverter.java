package com.demo.dronebackend.model;

import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.pojo.Alarm;

import java.util.Date;

public class AlarmConverter {
    /**
     * 用于将DroneReport中的相关信息转换并封装到Alarm对象中
     */
    public static Alarm fromReport(DroneReport report) {
        Alarm alarm = new Alarm();
        alarm.setDroneModel(report.getDroneModel());
        // 最后经纬度
        alarm.setLastLongitude(report.getLongitude());
        alarm.setLastLatitude(report.getLatitude());
        alarm.setLastAltitude(report.getHeight());
        // 飞手经纬度目前与最后经纬度一致
        alarm.setPilotLongitude(report.getLongitude());
        alarm.setPilotLatitude(report.getLatitude());
        alarm.setTakeoffTime(report.getIntrusionStartTime());
        Date landingTime = new Date(report.getIntrusionStartTime().getTime() + (long) (report.getLastingTime() * 1000));
        alarm.setLandingTime(landingTime);
        alarm.setIntrusionStartTime(report.getIntrusionStartTime());
        alarm.setDroneId(report.getStationId() + "-" + System.currentTimeMillis());
        alarm.setDroneSn(report.getDroneUUID());
        alarm.setFrequency(report.getFrequency());
        alarm.setBandwidth(report.getBandwidth());
        alarm.setSpeed(report.getSpeed());
        alarm.setHorizontalHeadingAngle(report.getHorizontalHeadingAngle());
        alarm.setVerticalHeadingAngle(report.getVerticalHeadingAngle());
        alarm.setType(report.getType());
        alarm.setScanids(report.getScanId());
        alarm.setScanid(report.getId());
        alarm.setLastingTime(report.getLastingTime());
        alarm.setBackLongitude(report.getBackLongitude());
        alarm.setBackLatitude(report.getBackLatitude());
        alarm.setTrajectory(report.getDrone());
        alarm.setStationId(report.getStationId());
        alarm.setDetectType(report.getDetectType());
        return alarm;
    }
}
