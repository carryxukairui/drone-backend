package com.demo.dronebackend.dto.screen;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyDroneStaDTO {
    private int hour;
    private long count;
}
