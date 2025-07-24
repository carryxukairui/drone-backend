package com.demo.dronebackend.dto.disposal;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class DisposalRecordQuery {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 10 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 10;

    /** 设备 ID */
    private String deviceId;

    /** 反制开始时间 ≥ counterStart */

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime counterStart;

    /** 反制结束时间 ≤ counterEnd */

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime counterEnd;
}
