package com.demo.dronebackend.dto.screen;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DeviceSettingReq {
    private double duration;
    private double power;
    @Min(0) @Max(1)
    private Integer g09OnOff;
    @Min(0) @Max(1)
    private Integer g16OnOff;
    @Min(0) @Max(1)
    private Integer g24OnOff;
    @Min(0) @Max(1)
    private Integer g58OnOff;
}
