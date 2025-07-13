package com.demo.dronebackend.dto.screen;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeekDroneStatsDTO {
    private String day;
    private long  count;
}
