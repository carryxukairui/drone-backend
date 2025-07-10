package com.demo.dronebackend.dto.screen;

import lombok.Data;

import java.util.List;

@Data
public class DeviceListDTO {
    private List<DeviceDTO> devices;
}
