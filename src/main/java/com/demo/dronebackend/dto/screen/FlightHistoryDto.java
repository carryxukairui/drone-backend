package com.demo.dronebackend.dto.screen;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class FlightHistoryDto {
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date takeoffTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date landingTime;
    private String droneId;
    private String droneSn;
    private String model;
    private String droneType;
    private Double frequency;
    private Double lastingTime;
    private Integer disposal;
    private Double pilotLongitude;
    private Double pilotLatitude;
    //起飞的
    private Double takeoffLongitude;
    private Double takeoffLatitude;
    //最后的
    private Double lastLongitude;
    private Double lastLatitude;
}
