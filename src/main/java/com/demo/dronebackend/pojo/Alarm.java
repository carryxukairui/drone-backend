package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.demo.dronebackend.handler.MapListTypeHandler;
import com.demo.dronebackend.handler.StringListTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @TableName alarm
 */
@TableName(value ="alarm")
@Data
public class Alarm {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String droneModel;

    private Double lastLongitude;

    private Double lastLatitude;

    private Double lastAltitude;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date takeoffTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date landingTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date intrusionStartTime;

    private String droneId;

    private String droneSn;

    private String droneType;

    private Double frequency;

    private Double bandwidth;

    private Double speed;

    private Double horizontalHeadingAngle;

    private Double verticalHeadingAngle;

    private Integer type;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object scanids;

    private String scanid;

    private Double lastingTime;

    private Double backLongitude;

    private Double backLatitude;

    private Double pilotLongitude;

    private Double pilotLatitude;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object trajectory;

    private String stationId;

    private Integer detectType;
}