package com.demo.dronebackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse {
    private String msg;
    private String status;
    private GeocodeLocation location;
}