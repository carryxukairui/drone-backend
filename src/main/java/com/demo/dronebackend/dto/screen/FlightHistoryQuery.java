package com.demo.dronebackend.dto.screen;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
public class FlightHistoryQuery {
    /** 起飞时间起始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /** 起飞时间结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /** 地图框选或行政区划，前端可传 GeoJSON 或行政区码 */
    private Map<String, Object> region;
    /** 无人机编号 */
    private String droneId;
    /** 无人机序列号 */
    private String droneSn;
    /** 无人机型号 */
    private String model;
    /** 国标/自定义类型 */
    private String droneType;
    /** 是否已反制 */
    private Boolean disposalFlag;

    /** 页码，默认 1 */
    @Min(1)
    private Integer page = 1;
    /** 每页条数，默认 10 */
    @Min(1)
    private Integer size = 10;
}