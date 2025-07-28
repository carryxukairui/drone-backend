package com.demo.dronebackend.dto.hardware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
// 设备状态上报数据结构
public class DeviceReport {
    @JsonIgnore
    private String vendor;
    private String stationId;
    private List<String> scannerD;
    private String id;
    private Integer linkState;
    private Double dataRate;
    private Integer foundTarget;
    private Double lng;
    private Double lat;
    private String ip;
}