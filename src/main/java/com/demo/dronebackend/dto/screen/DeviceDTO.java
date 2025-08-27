package com.demo.dronebackend.dto.screen;

import lombok.Data;

import java.util.Date;

@Data
public class DeviceDTO{
    private String deviceId;
    private String deviceName;
    private Double coverRange;
    private Double power;
    private Integer linkStatus;
    private Double longitude;
    private Double latitude;
    private String location;
    private String deviceType;
    private Date reportTime;
    private Double temperature;
}