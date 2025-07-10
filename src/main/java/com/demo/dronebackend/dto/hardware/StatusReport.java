package com.demo.dronebackend.dto.hardware;

import lombok.Data;

import java.util.List;

@Data
// 设备状态上报数据结构
public class StatusReport {
    private String stationId;
    private List<Scanner> scannerD;

}