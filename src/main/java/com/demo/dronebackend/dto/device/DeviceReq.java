package com.demo.dronebackend.dto.device;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceReq {

    /**
     * 设备 ID，由前端传入
     */
    @NotBlank(message = "设备 ID 不能为空")
    private String id;

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    /**
     * 设备类型：TDOA、RADAR、JAMMER 等
     */
    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    /**
     * 干扰设备覆盖距离（米）
     */
    @NotNull(message = "coverRange 不能为空")
    private Double coverRange;

    /**
     * 设备功率
     */
    @NotNull(message = "power 不能为空")
    private Double power;

    /**
     * 设备绑定的用户 ID
     */
    @NotNull(message = "deviceUserId 不能为空")
    private String deviceUserId;

    /**
     * 防空 ID，由前端传入
     */
    @NotBlank(message = "防空 ID 不能为空")
    private String stationId;
}
