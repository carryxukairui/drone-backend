package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.dto.hardware.DeviceReport;
import com.demo.dronebackend.dto.screen.DeviceSettingReq;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.pojo.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.Map;

/**
* @author 28611
* @description 针对表【device(设备表)】的数据库操作Service
* @createDate 2025-07-07 09:44:52
*/
public interface DeviceService extends IService<Device> {

    Result<?> addDevice(DeviceReq req);

    Result<?> updateDevice(DeviceReq req);

    Result<?> listDevices(DeviceQuery query);

    Result<?> deleteBatch(List<Long> ids);

    Map<String,Object> websocketDevice(DeviceReport report);

    Result<?> getDeviceDetail(String deviceId);

    Result<?> updateDeviceParamSettings(String deviceId, DeviceSettingReq parmSettings) throws MqttException;

    Result<?> listDisposalRecords(Integer page, Integer size);

    Result<?> getDeviceList();

    // 处理设备上报 硬件
    Map<String, Object> handleDeviceReport(DeviceConvertible deviceConvertible);
}
