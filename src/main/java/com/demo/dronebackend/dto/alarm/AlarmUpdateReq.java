package com.demo.dronebackend.dto.alarm;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmUpdateReq {

    /**
     * 入侵开始时间（ISO8601 格式）
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime intrusionTime;

    /**
     * 无人机型号
     */
    @NotBlank(message = "droneModel 不能为空")
    private String droneModel;

    /**
     * 侦测到的无人机唯一识别码
     */
    @NotBlank(message = "droneSn 不能为空")
    private String droneSn;

    /**
     * 类型，比如 0: 遥控器，1: 无人机
     */
    private Integer type;

    /**
     * 位置信息（此字段需与数据库字段对应，如无对应列可在表中新增）
     */
    private String location;
}
