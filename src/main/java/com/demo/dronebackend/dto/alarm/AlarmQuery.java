package com.demo.dronebackend.dto.alarm;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AlarmQuery {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 10 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 10;

    /** 无人机 ID */
    private Long droneId;

    /** 无人机型号（模糊匹配） */
    private String droneModel;

    /** 起飞时间 ≥ startTime */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    /** 降落时间 ≤ endTime */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    /** 防区 ID */
    private Long stationId;

    /** 探测类型 */
    private Integer detectType;
}
