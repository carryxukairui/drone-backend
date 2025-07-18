package com.demo.dronebackend.dto.screen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatisticsDTO {
    private Long todayAlarmCount;
    private Long totalAlarmCount;
    private Long todayDisposalCount;
    private Long totalDisposalCount;
}
