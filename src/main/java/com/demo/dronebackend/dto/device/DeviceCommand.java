package com.demo.dronebackend.dto.device;

import com.demo.dronebackend.dto.screen.DeviceSettingReq;
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
    private int g52_onoff;
    private int g58_onoff;
    public DeviceCommand(String deviceID, DeviceSettingReq  req){
        this.deviceID = deviceID;
        this.g09_onoff = req.getG09OnOff();
        this.g16_onoff = req.getG16OnOff();
        this.g24_onoff = req.getG24OnOff();
        this.g52_onoff = req.getG52OnOff();
        this.g58_onoff = req.getG58OnOff();
    }
}
