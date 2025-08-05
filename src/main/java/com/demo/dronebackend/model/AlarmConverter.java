package com.demo.dronebackend.model;

import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.pojo.Alarm;

import java.util.*;

public class AlarmConverter {
    /**
     * 用于将DroneReport中的相关信息转换并封装到Alarm对象中
     */
    public static Alarm fromReport(DroneReport report) {
        Alarm alarm = new Alarm();

        Date now = new Date();
        if (report.getIntrusion_start_time() != null) {
            now = report.getIntrusion_start_time();
        }
        alarm.setDroneModel(report.getModel());
        // 最后经纬度
        alarm.setLastLongitude(report.getLongitude());
        alarm.setLastLatitude(report.getLatitude());
        alarm.setLastAltitude(report.getHeight());
        // 飞手经纬度目前与最后经纬度一致
        alarm.setPilotLongitude(report.getLongitude());
        alarm.setPilotLatitude(report.getLatitude());
        alarm.setIntrusionStartTime(now);
        alarm.setTakeoffTime(now);


        Date landingTime = new Date(now.getTime() + (long) (report.getLastingTime() * 1000));
        alarm.setLandingTime(landingTime);

        alarm.setDroneId(report.getStation_id() + "-" + System.currentTimeMillis());
        alarm.setDroneSn(report.getDrone_uuid());
        alarm.setFrequency(report.getFrequency());
        alarm.setBandwidth(report.getBandwidth());
        alarm.setSpeed(report.getSpeed());
        alarm.setHorizontalHeadingAngle(report.getHorizontal_heading_angle());
        alarm.setVerticalHeadingAngle(report.getVertical_heading_angle());
        alarm.setType(report.getType());
        alarm.setScanids(report.getScanId());
        alarm.setScanid(report.getId());
        alarm.setLastingTime(report.getLastingTime());
        alarm.setBackLongitude(report.getBackLongitude());
        alarm.setBackLatitude(report.getBackLatitude());
        //TODO:业务逻辑修改
        List<Map<String,Double>> trajectory = new ArrayList<>();
        Map<String,Double> point = new HashMap<>();
        point.put("lon",report.getLongitude());
        point.put("lat",report.getLatitude());
        trajectory.add(point);
        alarm.setTrajectory(trajectory);
        alarm.setStationId(report.getStation_id());
        alarm.setDetectType(report.getDetect_type());
        return alarm;
    }
}
