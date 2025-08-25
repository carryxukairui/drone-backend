package com.demo.dronebackend.dto.hardware;


import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.pojo.Alarm;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 默认硬件上报侦测数据
 * 字段要与json数据中字段名称保持一致
 */
@Data
public class DefaultDroneReport implements AlarmConvertible {

    private String station_id;

    private Integer detect_type;

    private String intrusion_start_time;

    private Double longitude;

    private Double latitude;

    private Double height;

    private Double frequency;

    private Double bandwidth;

    private Double speed;

    private Double horizontal_heading_angle;

    private Double vertical_heading_angle;

    private String model;

    private Integer type;

    private Double lasting_time;

    // 设备id集合
    private List<ScanID> scanID;

    // 设备id
    private String id;

    private String drone_uuid;

    private Double back_longitude;

    private Double back_latitude;
    //操作员位置纬度
    @JsonProperty("Op_Lon")
    private Double Op_Lon;
    @JsonProperty("Op_Lat")
    private Double Op_Lat;

    @Data
    public static class ScanID {
        private String id;
    }

    @Override
    public Alarm toAlarm() {
        // String intrusion_start_time ---> Date intrusionStartTime
        Date intrusionStartTime = null;
        try {
            intrusionStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(this.intrusion_start_time);
        } catch (ParseException e) {
            throw new BusinessException(e.getMessage());
        }

        Alarm alarm = new Alarm();
        alarm.setDroneModel(this.model);
        alarm.setLastLongitude(this.longitude);
        alarm.setLastLatitude(this.latitude);
        alarm.setLastAltitude(this.height);
        alarm.setTakeoffTime(intrusionStartTime);
        Date landingTime = new Date(intrusionStartTime.getTime() + (long) (this.getLasting_time() * 1000));
        alarm.setLandingTime(landingTime);
        alarm.setIntrusionStartTime(intrusionStartTime);
        alarm.setDroneId(this.getStation_id() + "-" + intrusionStartTime.getTime());
        alarm.setDroneSn(this.drone_uuid);
        alarm.setFrequency(this.frequency);
        alarm.setBandwidth(this.bandwidth);
        alarm.setSpeed(this.speed);
        alarm.setHorizontalHeadingAngle(this.horizontal_heading_angle);
        alarm.setVerticalHeadingAngle(this.vertical_heading_angle);
        alarm.setType(this.type);
        alarm.setScanids(this.scanID);
        alarm.setScanid(this.id);
        alarm.setLastingTime(this.lasting_time);
        // 返航经纬度与飞手经纬度一致
        alarm.setBackLongitude(this.Op_Lon);
        alarm.setBackLatitude(this.Op_Lat);
        alarm.setPilotLongitude(this.Op_Lon);
        alarm.setPilotLatitude(this.Op_Lat);
        // 设置轨迹
        List<Map<String, Double>> trajectory = new ArrayList<>();
        Map<String, Double> point = new HashMap<>();
        point.put("lng", this.getLongitude());
        point.put("lat", this.getLatitude());
        trajectory.add(point);
        alarm.setTrajectory(trajectory);
        alarm.setStationId(this.station_id);
        alarm.setDetectType(this.detect_type);
        return alarm;
    }
}
