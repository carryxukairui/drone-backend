package com.demo.dronebackend.dto.hardware;

import lombok.Data;

//设备状态
@Data
public class Scanner {
    private String id;
    private Integer linkState;
    private Double dataRate;
    private Integer foundTarget;
    private Double lng;
    private Double lat;
    private String ip;


}