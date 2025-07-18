package com.demo.dronebackend.dto.screen;


import lombok.Data;

import java.util.Date;

@Data
public class AlertRegionDTO {
    private Long id;
    private Date time;
    private Integer type;
    private Double centerLon;
    private Double centerLat;
    private Double radius;
    private String location;
}
