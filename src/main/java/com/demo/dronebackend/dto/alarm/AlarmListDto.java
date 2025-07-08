package com.demo.dronebackend.dto.alarm;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlarmListDto {
    private List<AlarmDto> records;
}
