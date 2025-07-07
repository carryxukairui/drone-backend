package com.demo.dronebackend.dto.device;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DeviceQuery {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 10 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 10;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型 */
    private String deviceType;

    /** 绑定用户 ID */
    private Long deviceUserId;

    /** 防区 ID */
    private String stationId;

    /** 工作状态：0 异常，1 正常 */
    private Integer linkStatus;
}
