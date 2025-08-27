package com.demo.dronebackend.pojo;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @TableName device
 */
@TableName(value ="device")
@Data
public class Device {
    private String id;

    private String deviceName;

    private String deviceType;

    private Long deviceUserId;

    private Object deviceStatus;

    private String stationId;

    private Integer linkStatus;

    private Double longitude;

    private Double latitude;

    private String ip;

    private Double coverRange;

    private Double power;
    private Date reportTime;
    private Double temperature;
}