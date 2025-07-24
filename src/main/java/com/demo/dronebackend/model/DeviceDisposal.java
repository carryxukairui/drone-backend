package com.demo.dronebackend.model;

import com.demo.dronebackend.pojo.Device;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class DeviceDisposal {
    private Instant start;
    private Instant end;
    private Device device;
}
