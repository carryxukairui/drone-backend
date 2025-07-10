package com.demo.dronebackend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceCommand {
    private String deviceID;
    private int g09_onoff;
    private int g16_onoff;
    private int g24_onoff;
    private int g58_onoff;
}
