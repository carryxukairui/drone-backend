package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * @TableName alarm
 */
@TableName(value ="alarm")
@Data
public class Alarm {
    private Long id;

    private String droneModel;

    private Double lastLongitude;

    private Double lastLatitude;

    private Double lastAltitude;

    private LocalDateTime takeoffTime;

    private LocalDateTime landingTime;

    private LocalDateTime intrusionStartTime;

    private String droneId;

    private String droneSn;

    private String droneType;

    private Double frequency;

    private Double bandwidth;

    private Double speed;

    private Double horizontalHeadingAngle;

    private Double verticalHeadingAngle;

    private Integer type;

    private Object scanids;

    private String scanid;

    private Double lastingTime;

    private Double backLongitude;

    private Double backLatitude;

    private Double pilotLongitude;

    private Double pilotLatitude;

    private Object trajectory;

    private String stationId;

    private Integer detectType;
}