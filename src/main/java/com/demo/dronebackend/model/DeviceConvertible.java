package com.demo.dronebackend.model;

import com.demo.dronebackend.pojo.Device;

/*
适配器接口，将设备上报数据转换成设备对象
 */
public interface DeviceConvertible {
    Device toDevice();
}
