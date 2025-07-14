package com.demo.dronebackend.dto.alarm;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;


import java.util.Date;

@Data
public class AlarmDTO {
    private Long id;
    private String droneModel;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date intrusionTime;
    private String location;
    private String type;
    private String droneSn;
}
