package com.demo.dronebackend.dto.alarm;

import lombok.Data;

import java.util.Date;

@Data
public class AlarmDTO {
    private Long id;
    private String droneModel;
    private Date intrusionTime;
    private String location;
    private String type;
    private String droneSn;
}
