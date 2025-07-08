package com.demo.dronebackend.dto.alarm;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmDto {
    private String id;
    private String droneModel;
    private LocalDateTime intrusionTime;
    private String location;
    private String type;
    private String droneSn;
}
