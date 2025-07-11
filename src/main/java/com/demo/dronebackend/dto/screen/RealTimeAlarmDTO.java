package com.demo.dronebackend.dto.screen;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class RealTimeAlarmDTO {
    private Long id;
    private String droneModel;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date intrusionTime;
    private String location;
    private String type;
    private String droneSn;
}
