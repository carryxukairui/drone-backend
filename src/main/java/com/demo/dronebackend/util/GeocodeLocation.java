package com.demo.dronebackend.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeLocation {
    private double lon;
    private double lat;
    private int    score;
    private String level;
    private String keyWord;
}
