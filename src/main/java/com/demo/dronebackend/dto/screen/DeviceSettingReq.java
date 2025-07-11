package com.demo.dronebackend.dto.screen;


import lombok.Data;

@Data
public class DeviceSettingReq {
    private double duration;
    private double power;
    private Integer g09OnOff;
    private Integer g16OnOff;
    private Integer g24OnOff;
    private Integer g58OnOff;
}
