package com.demo.dronebackend.dto.screen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorCountDTO {
    private Long todayCount;

    private String yoy;

    private String dod;
}
