package com.demo.dronebackend.dto.screen;

import lombok.Data;

@Data
public class DeviceDTO{
    private String deviceId;
    private String deviceName;
    private Double coverRange;
    private Double power;
    private Integer linkStatus;
    private String location;
}