package com.demo.dronebackend.dto.screen;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class RealtimeAlarmReq {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 20 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 20;

    /** 最近显示条数，默认100 */
    @Min(value = 1, message = "最近显示条数必须 ≥ 1")
    private Integer size_limit=100;

    /** 起飞时间 ≥ startTime */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 降落时间 ≤ endTime */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 无人机型号（模糊匹配） */
    private String droneModel;

    /** 无人机类型（黑白名单） */
    private String type;

}
