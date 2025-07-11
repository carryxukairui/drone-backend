package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.dto.hardware.Scanner;
import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.dto.screen.DeviceDTO;
import com.demo.dronebackend.dto.screen.DeviceListDTO;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.util.CurrentUserContext;
import com.demo.dronebackend.ws.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author 28611
* @description 针对表【device(设备表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device>
    implements DeviceService{

    private final DeviceMapper deviceMapper;
    private final WebSocketService webSocketService;

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
        return Result.success( null);
    }

    @Override
    public Map<String ,Object> websocketDevice(StatusReport report) {
        // 1. 收集所有上报里的设备ID
        List<String> deviceIds = report.getScannerD()
                .stream()
                .map(Scanner::getId)
                .toList();

        List<Device> devices = deviceMapper.selectBatchIds(deviceIds);

        Map<String, List<DeviceDTO>> dtoByUser = new HashMap<>();
        for (Device dev : devices) {
            String userId = String.valueOf(dev.getDeviceUserId());

            DeviceDTO dto = new DeviceDTO();
            dto.setDeviceId(dev.getId());
            dto.setDeviceName(dev.getDeviceName());
            dto.setCoverRange(dev.getCoverRange());
            dto.setPower(dev.getPower());
            dto.setLinkStatus(dev.getLinkStatus());
            dto.setDeviceType(dev.getDeviceType());
            //TODO：接入api获取位置信息
            dto.setLocation("详细地址");
            // 加入到对应用户的列表
            dtoByUser.computeIfAbsent(userId, k -> new ArrayList<>())
                    .add(dto);
        }
        // 4. 分用户推送：为每个用户构造子报告并发送
        dtoByUser.forEach(webSocketService::sendDeviceListToUser);

        return Map.of("code", 200, "msg", "Success");
    }

}




