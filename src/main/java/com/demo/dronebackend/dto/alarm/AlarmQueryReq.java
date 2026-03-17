package com.demo.dronebackend.dto.alarm;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
public class AlarmQueryReq {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 10 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 10;

    /** 无人机序列号 */
    private String droneSn;

    /** 无人机型号（模糊匹配） */
    private String droneModel;

    /** 起飞时间 ≥ startTime */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 降落时间 ≤ endTime */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 防区 ID */
    private Long stationId;

    /** 探测类型 */
    private Integer detectType;

    //设备 ID
    private String deviceId;
}
