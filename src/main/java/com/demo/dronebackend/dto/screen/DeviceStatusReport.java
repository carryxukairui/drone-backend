package com.demo.dronebackend.dto.screen;

import java.util.Map;

// 接收硬件上报的 JSON 格式
public record DeviceStatusReport(
    Long deviceId,
    String deviceType,
    Double latitude,
    Double longitude,
    Double coverRange
) {}
