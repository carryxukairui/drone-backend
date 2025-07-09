package com.demo.dronebackend.dto.screen;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightHistoryDto {
    private LocalDateTime takeoffTime;
    private LocalDateTime landingTime;
    private String droneId;
    private String droneSn;
    private String model;
    private String droneType;
    private Double frequency;
    private Double lastingTime;
    private Boolean disposal;
    private Double pilotLongitude;
    private Double pilotLatitude;
    private Double takeoffLongitude;
    private Double takeoffLatitude;
    private Double lastLongitude;
    private Double lastLatitude;
}
