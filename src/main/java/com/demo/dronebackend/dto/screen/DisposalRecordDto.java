package com.demo.dronebackend.dto.screen;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class DisposalRecordDto {
    private Long id;              // 操作记录 ID
    private String operator;      // 操作者姓名
    private String deviceName;    // 设备名称
    private String commandTime;   // 反制时间，格式 yyyy-MM-dd HH:mm:ss
    private String deviceType;    // 设备类型
    private Integer unattended;   // 无人值守状态：0/1
    private SwitchStatusDto switchStatus;  // 开关状态描述

    public void setSwitchStatus(boolean g09Onoff, boolean g16Onoff, boolean g24Onoff, boolean g58Onoff) {
        this.switchStatus = new SwitchStatusDto(g09Onoff, g16Onoff, g24Onoff, g58Onoff);
    }
}
@Data
@AllArgsConstructor
 class SwitchStatusDto {
    private boolean g09;  // 0.9G
    private boolean g16;  // 1.6G
    private boolean g24;  // 2.4G
    private boolean g58;  // 5.8G
}
