package com.demo.dronebackend.dto.screen;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class DetailedAlarmDTO {

    private Long id;

    private String droneId;

    private String droneSn;

    private String droneModel;

    private Date takeoffTime;

    private Date landingTime;

    private Date intrusionStartTime;

    private Double frequency;

    private Double lastingTime;

    private Double speed;

    private Double pilotLongitude;

    private Double pilotLatitude;

    private Double lastLongitude;

    private Double lastLatitude;

    private Double lastAltitude;

    private Object trajectory;

}
