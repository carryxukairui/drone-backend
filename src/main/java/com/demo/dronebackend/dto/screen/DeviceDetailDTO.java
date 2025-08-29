package com.demo.dronebackend.dto.screen;

import com.demo.dronebackend.pojo.Device;
import lombok.Data;

@Data
public class DeviceDetailDTO {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Double coverRange;
    private Double power;
    private Integer linkStatus;
    private Double latitude;
    private Double longitude;
    private Double temperature;

    public DeviceDetailDTO(Device  device){
        this.deviceId = device.getId();
        this.deviceName = device.getDeviceName();
        this.deviceType = device.getDeviceType();
        this.coverRange = device.getCoverRange();
        this.power = device.getPower();
        this.linkStatus = device.getLinkStatus();
        this.latitude = device.getLatitude();
        this.longitude = device.getLongitude();
        this.temperature = device.getTemperature();
    }
}
