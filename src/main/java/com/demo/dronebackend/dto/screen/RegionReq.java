package com.demo.dronebackend.dto.screen;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class RegionReq {
    /** 中心点经度 */
    @NotNull
    private Double centerLon;
    /** 中心点纬度 */
    @NotNull
    private Double centerLat;
    /** 区域半径，单位：公里 */
    @NotNull
    private Double radius;
    /** 告警类型 */
    @NotNull
    @Range(min = 0, max = 2)
    private Integer alertType;
}
