package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.dto.hardware.Scanner;
import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.DeviceDTO;
import com.demo.dronebackend.dto.screen.DeviceDetailDTO;
import com.demo.dronebackend.dto.screen.DeviceSettingReq;
import com.demo.dronebackend.dto.screen.DisposalRecordDto;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.mapper.DisposalRecordMapper;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.pojo.DisposalRecord;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.service.MqttService;
import com.demo.dronebackend.service.TiandituService;
import com.demo.dronebackend.util.CurrentUserContext;
import com.demo.dronebackend.ws.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.demo.dronebackend.constant.SystemConstants.DEVICES_WEBSOCKET_TOPIC;

/**
 * @author 28611
 * @description 针对表【device(设备表)】的数据库操作Service实现
 * @createDate 2025-07-07 09:44:52
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device>
        implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DisposalRecordMapper disposalRecordMapper;
    private final UserMapper userMapper;
    private final WebSocketService webSocketService;
    private final TiandituService tiandituService;
    private final MqttService mqttService;
    private static final String topic = "device/command/startJam";

    @Override
    public Result<?> addDevice(DeviceReq req) {
        Long reqUserid = Long.valueOf(req.getDeviceUserId());

        Device existDevice = deviceMapper.selectById(req.getId());
        if (existDevice != null) {
            return Result.error("设备已存在");
        }

        Device device = new Device();
        device.setId(req.getId());
        device.setDeviceName(req.getDeviceName());
        device.setDeviceType(req.getDeviceType());
        device.setCoverRange(req.getCoverRange());
        device.setPower(req.getPower());
        device.setDeviceUserId(reqUserid);


        deviceMapper.insert(device);

        return Result.success("添加设备成功");
    }

    @Override
    public Result<?> updateDevice(DeviceReq req) {
        Device device = deviceMapper.selectById(req.getId());
        if (device == null) {
            return Result.error("设备不存在");
        }

        device.setDeviceName(req.getDeviceName());
        device.setDeviceType(req.getDeviceType());
        device.setCoverRange(req.getCoverRange());
        device.setPower(req.getPower());
        device.setDeviceUserId(Long.valueOf(req.getDeviceUserId()));

        deviceMapper.updateById(device);
        return Result.success("更新设备成功");
    }

    @Override
    public Result<?> listDevices(DeviceQuery q) {
        Page<Device> page = new Page<>(q.getPage(), q.getSize());

        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getDeviceName())) {
            qw.like(Device::getDeviceName, q.getDeviceName());
        }
        if (StringUtils.hasText(q.getDeviceType())) {
            qw.eq(Device::getDeviceType, q.getDeviceType());
        }
        if (StringUtils.hasText(q.getStationId())) {
            qw.eq(Device::getStationId, q.getStationId());
        }
        if (q.getLinkStatus() != null) {
            qw.eq(Device::getLinkStatus, q.getLinkStatus());
        }
        if (q.getDeviceUserId() != null) {
            qw.eq(Device::getDeviceUserId, q.getDeviceUserId());
        }

        User me = CurrentUserContext.get();
        //普通用户
        if (!PermissionType.admin.getDesc().equals(me.getPermission())) {
            qw.eq(Device::getDeviceUserId, me.getId());
        }

        Page<Device> devicePage = deviceMapper.selectPage(page, qw);
        MyPage<Device> myPage = new MyPage<>(devicePage);
        return Result.success(myPage);
    }

    @Override
    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除失败：ID 列表为空");
        }
        int r = deviceMapper.deleteBatchIds(ids);
        if (r == 0) {
            throw new BusinessException("未删除任何记录，请检查 ID 是否正确");
        }
        return Result.success(null);
    }

    @Override
    public Map<String, Object> websocketDevice(StatusReport report) {


        Device dev = deviceMapper.selectById(report.getId());
        if(dev == null)
            return null;
        String userId = String.valueOf(dev.getDeviceUserId());
        dev.setLongitude(report.getLng());
        dev.setLatitude(report.getLat());
        dev.setLinkStatus(report.getLinkState());
        dev.setIp(report.getIp());
        dev.setStationId(report.getStationId());

        deviceMapper.updateById(dev);

        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(dev.getId());
        dto.setDeviceName(dev.getDeviceName());
        dto.setCoverRange(dev.getCoverRange());
        dto.setPower(dev.getPower());
        dto.setLinkStatus(dev.getLinkStatus());
        dto.setDeviceType(dev.getDeviceType());
        dto.setLongitude(dev.getLongitude());
        dto.setLatitude(dev.getLatitude());
        String location = tiandituService.reverseGeocode(dev.getLongitude(), dev.getLatitude());
        dto.setLocation(location);


        String deviceTopic = DEVICES_WEBSOCKET_TOPIC + ":" + userId;
        webSocketService.sendDeviceListToUser(deviceTopic, dto);


        return Map.of("code", 200, "msg", "Success");
    }

    @Override
    public Result<?> getDeviceDetail(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return Result.error("设备不存在");
        }

        return Result.success(new DeviceDetailDTO(device));
    }

    @Override
    public Result<?> updateDeviceParamSettings(String deviceId, DeviceSettingReq paramSettings) throws MqttException {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return Result.error("设备不存在");
        }

        device.setPower(paramSettings.getPower());
        deviceMapper.updateById(device);

        DisposalRecord dr = disposalRecordMapper.selectOne(
                new LambdaQueryWrapper<DisposalRecord>()
                        .eq(DisposalRecord::getDeviceId, deviceId)
        );
        if (dr == null) {
            dr = new DisposalRecord();
            dr.setDeviceId(deviceId);
        }
        dr.setG09Onoff(paramSettings.getG09OnOff());
        dr.setG16Onoff(paramSettings.getG16OnOff());
        dr.setG24Onoff(paramSettings.getG24OnOff());
        dr.setG58Onoff(paramSettings.getG58OnOff());
        dr.setDuration(paramSettings.getDuration());
        disposalRecordMapper.updateById(dr);


        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("deviceID", deviceId);
        payloadMap.put("g09_onoff", dr.getG09Onoff());
        payloadMap.put("g16_onoff", dr.getG16Onoff());
        payloadMap.put("g24_onoff", dr.getG24Onoff());
        payloadMap.put("g58_onoff", dr.getG58Onoff());
        payloadMap.put("duration", dr.getDuration());
        //TODO: 将设备修改信息通过MQTT发送给硬件
        try {
            String payload = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(payloadMap);
            mqttService.publish(topic, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success(null);
    }

    @Override
    public Result<?> listDisposalRecords(Integer page, Integer size) {
        long userId = StpUtil.getLoginIdAsLong();
        Page<DisposalRecord> pr = new Page<>(page, size);
        List<String> deviceIds = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceUserId, userId)
        ).stream().map(Device::getId).toList();

        if (deviceIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        LambdaQueryWrapper<DisposalRecord> qw = new LambdaQueryWrapper<>();
        qw.in(DisposalRecord::getDeviceId, deviceIds)
                .orderByDesc(DisposalRecord::getTime);
        Page<DisposalRecord> pageResult = disposalRecordMapper.selectPage(pr, qw);

        List<DisposalRecordDto> dtoList = pageResult.getRecords().stream().map(r -> {
            DisposalRecordDto dto = new DisposalRecordDto();
            Device device = deviceMapper.selectById(r.getDeviceId());
            dto.setId(r.getId());
            dto.setOperator(userMapper.selectById(userId).getName());
            dto.setDeviceName(device.getDeviceName());
            dto.setDeviceType(device.getDeviceType());
            dto.setCommandTime(
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(r.getTime())
            );
            dto.setUnattended(r.getG09Onoff()); // 或者根据业务设定
            // 拼接 switch 状态
            dto.setSwitchStatus(r.getG09Onoff() == 1, r.getG16Onoff() == 1,
                    r.getG24Onoff() == 1, r.getG58Onoff() == 1);
            return dto;
        }).toList();
        MyPage<DisposalRecordDto> resultPage = new MyPage<>(
                pageResult.getCurrent(),
                pageResult.getPages(),
                pageResult.getSize(),
                pageResult.getTotal(),
                dtoList, null
        );
        return Result.success(resultPage);
    }

    @Override
    public Result<List<DeviceDTO>> getDeviceList() {
        long userId = StpUtil.getLoginIdAsLong();


        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceUserId, userId));

        List<DeviceDTO> dtos = devices.stream().map(d -> {
            DeviceDTO dto = new DeviceDTO();
            dto.setDeviceId(d.getId());
            dto.setDeviceName(d.getDeviceName());
            dto.setDeviceType(d.getDeviceType());
            dto.setCoverRange(d.getCoverRange());
            dto.setPower(d.getPower());
            dto.setLinkStatus(d.getLinkStatus());
            dto.setLatitude(d.getLatitude());
            dto.setLongitude(d.getLongitude());
            String location = tiandituService.reverseGeocode(d.getLongitude(), d.getLatitude());
            dto.setLocation(location);
            return dto;
        }).collect(Collectors.toList());


        return Result.success(dtos);
    }


}




