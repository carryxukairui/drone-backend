package com.demo.dronebackend.dto.screen;

import lombok.Data;

import java.util.Date;

@Data
public class RealTimeAlarmDTO {
    private Long id;
    private String droneModel;
    private Date intrusionTime;
    private String location;
    private String type;
    private String droneSn;
    private Double longitude;
    private Double latitude;
}
