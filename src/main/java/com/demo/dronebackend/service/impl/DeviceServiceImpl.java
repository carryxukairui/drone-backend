package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.constant.SystemConstants;
import com.demo.dronebackend.dto.device.DeviceCommand;
import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.dto.hardware.DeviceReport;
import com.demo.dronebackend.dto.screen.*;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.mapper.DisposalRecordMapper;
import com.demo.dronebackend.mapper.SystemLogMapper;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.util.GeoEntry;
import com.demo.dronebackend.service.*;
import com.demo.dronebackend.util.MyPage;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.pojo.DisposalRecord;
import com.demo.dronebackend.pojo.SystemLog;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.model.DeviceDisposalManager;
import com.demo.dronebackend.ws.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static com.demo.dronebackend.constant.SystemConstants.DEVICES_WEBSOCKET_TOPIC;
import static com.demo.dronebackend.constant.SystemConstants.UNATTENDED_WEBSOCKET_TOPIC;
import static com.demo.dronebackend.constant.SystemLogConstants.DEVICE_DISPOSAL_EVENT;
import static com.demo.dronebackend.constant.SystemLogConstants.OP_TYPE_UNATTENDED_EVENT;

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
    private final MqttService mqttService;
    private final UnattendedService unattendedService;
    private final SystemLogMapper systemLogMapper;
    private final DeviceDisposalManager deviceDisposalManager;
    private final TaskScheduler taskScheduler;
    private final GeoCacheServiceImpl geoCacheService;
    private static  final  Integer OFFLINE_CHECK_INTERVAL =  20 * 1000;
    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 存储每个设备对应的“离线检测”定时任务
    private Map<String, ScheduledFuture<?>> offlineTasks;

    @PostConstruct
    public void init() {
        offlineTasks = new ConcurrentHashMap<>();
    }

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
        if (StrUtil.isNotBlank(q.getDeviceName())) {
            qw.like(Device::getDeviceName, q.getDeviceName());
        }
        if (StrUtil.isNotBlank(q.getDeviceType())) {
            qw.eq(Device::getDeviceType, q.getDeviceType());
        }
        if (StrUtil.isNotBlank(q.getStationId())) {
            qw.eq(Device::getStationId, q.getStationId());
        }
        Integer linkStatus = q.getLinkStatus();
        if (linkStatus != null && (linkStatus == 0 || linkStatus == 1)) {
            qw.eq(Device::getLinkStatus, linkStatus);
        }
        if (StrUtil.isNotBlank(q.getDeviceUserId())) {
            qw.eq(Device::getDeviceUserId, q.getDeviceUserId());
        }

        long userId = StpUtil.getLoginIdAsLong();
        User me = userMapper.selectById(userId);
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
        return Result.success("批量删除成功");
    }
    @Override
    public Map<String, Object> handleDeviceReport(DeviceConvertible report) {
        Device devFromReport  = report.toDevice();
        Device existingDevice = deviceMapper.selectById(devFromReport.getId());
        if (existingDevice == null) {
            return Map.of("code", 404, "msg", "Device not found");
        }
        // 只更新非空字段
        if (devFromReport.getStationId() != null) {
            existingDevice.setStationId(devFromReport.getStationId());
        }
        if (devFromReport.getLinkStatus() != null) {
            existingDevice.setLinkStatus(devFromReport.getLinkStatus());
        }
        if (devFromReport.getLongitude() != null) {
            existingDevice.setLongitude(devFromReport.getLongitude());
        }
        if (devFromReport.getLatitude() != null) {
            existingDevice.setLatitude(devFromReport.getLatitude());
        }
        if (devFromReport.getIp() != null) {
            existingDevice.setIp(devFromReport.getIp());
        }
        if (devFromReport.getTemperature() != null){
            existingDevice.setTemperature(devFromReport.getTemperature());
        }
        existingDevice.setReportTime(new Date());
        deviceMapper.updateById(existingDevice);


        DeviceDTO dto = buildDtoFromDevice(existingDevice);
        String userId = String.valueOf(existingDevice.getDeviceUserId());
        String deviceId = existingDevice.getId();
        String deviceTopic = DEVICES_WEBSOCKET_TOPIC + ":" + userId;
        webSocketService.sendDeviceListToUser("device", deviceTopic, dto);


        // —— 2. 维护“离线定时器” ——
        // 如果之前已有一个未执行的“离线任务”，先取消它
        ScheduledFuture<?> prev = offlineTasks.get(deviceId);
        if (prev != null && !prev.isDone()) {
            prev.cancel(false);
        }
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> markOffline(deviceId, deviceTopic),
                new Date(System.currentTimeMillis() + OFFLINE_CHECK_INTERVAL)
        );
        offlineTasks.put(deviceId, future);
        //无人值守逻辑
        User user = userMapper.selectById(userId);

        if (user != null && Integer.valueOf(1).equals(user.getUnattended())) {
            List<SystemLog> logs = systemLogMapper.selectList(
                    new LambdaQueryWrapper<SystemLog>()
                            .eq(SystemLog::getUserId, userId)
                            .eq(SystemLog::getOperationType, OP_TYPE_UNATTENDED_EVENT)
                            .orderByDesc(SystemLog::getCreatedTime)
                            .last("LIMIT 20")
            );

            List<SystemLogDTO> logDtos = logs.stream()
                    .map(log -> {
                        SystemLogDTO dtos = new SystemLogDTO();
                        dtos.setOperationType(log.getOperationType());
                        LocalDateTime ldt = LocalDateTime.ofInstant(
                                log.getCreatedTime().toInstant(),
                                ZoneId.systemDefault()
                        );
                        dtos.setCreatedTime(ldt.format(DTF));
                        dtos.setDescription(log.getDescription());
                        return dtos;
                    })
                    .toList();

            String noAttendedTopic = UNATTENDED_WEBSOCKET_TOPIC + ":" + userId;
            webSocketService.sendDeviceListToUser("noAttended", noAttendedTopic, logDtos);
        }
        return Map.of("code", 200, "msg", "Success");
    }
    @Override
    public Map<String, Object> websocketDevice(DeviceReport report) {
        Device dev = deviceMapper.selectById(report.getId());
        if (dev == null) {
            return Map.of("code", 404, "msg", "Device not found");
        }

        String userId = String.valueOf(dev.getDeviceUserId());
        String deviceId = dev.getId();

        dev.setLongitude(report.getLng());
        dev.setLatitude(report.getLat());
        dev.setLinkStatus(report.getLinkState());
        dev.setIp(report.getIp());
        dev.setStationId(report.getStationId());
        deviceMapper.updateById(dev);

        DeviceDTO dto = buildDtoFromDevice(dev);


        String deviceTopic = DEVICES_WEBSOCKET_TOPIC + ":" + userId;
        webSocketService.sendDeviceListToUser("device", deviceTopic, dto);


        // —— 2. 维护“离线定时器” ——
        // 如果之前已有一个未执行的“离线任务”，先取消它
        ScheduledFuture<?> prev = offlineTasks.get(deviceId);
        if (prev != null && !prev.isDone()) {
            prev.cancel(false);
        }
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> markOffline(deviceId, deviceTopic),
                new Date(System.currentTimeMillis() + OFFLINE_CHECK_INTERVAL)
        );
        offlineTasks.put(deviceId, future);
        //无人值守逻辑
        User user = userMapper.selectById(userId);

        if (user != null && Integer.valueOf(1).equals(user.getUnattended())) {
            List<SystemLog> logs = systemLogMapper.selectList(
                    new LambdaQueryWrapper<SystemLog>()
                            .eq(SystemLog::getUserId, userId)
                            .eq(SystemLog::getOperationType, OP_TYPE_UNATTENDED_EVENT)
                            .orderByDesc(SystemLog::getCreatedTime)
                            .last("LIMIT 20")
            );

            List<SystemLogDTO> logDtos = logs.stream()
                    .map(log -> {
                        SystemLogDTO dtos = new SystemLogDTO();
                        dtos.setOperationType(log.getOperationType());
                        LocalDateTime ldt = LocalDateTime.ofInstant(
                                log.getCreatedTime().toInstant(),
                                ZoneId.systemDefault()
                        );
                        dtos.setCreatedTime(ldt.format(DTF));
                        dtos.setDescription(log.getDescription());
                        return dtos;
                    })
                    .toList();

            String noAttendedTopic = UNATTENDED_WEBSOCKET_TOPIC + ":" + userId;
            webSocketService.sendDeviceListToUser("noAttended", noAttendedTopic, logDtos);
        }
        return Map.of("code", 200, "msg", "Success");
    }


    //设备离线
    private void markOffline(String deviceId, String deviceTopic) {
        Device dev = deviceMapper.selectById(deviceId);
        if (dev != null) {
            dev.setLinkStatus(0);
            deviceMapper.updateById(dev);

            DeviceDTO dto = buildDtoFromDevice(dev);
            webSocketService.sendDeviceListToUser("device", deviceTopic, dto);
        }
        offlineTasks.remove(deviceId);
    }

    private DeviceDTO buildDtoFromDevice(Device dev) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(dev.getId());
        dto.setDeviceName(dev.getDeviceName());
        dto.setCoverRange(dev.getCoverRange());
        dto.setPower(dev.getPower());
        dto.setLinkStatus(dev.getLinkStatus());
        dto.setDeviceType(dev.getDeviceType());
        dto.setLongitude(dev.getLongitude());
        dto.setLatitude(dev.getLatitude());
        //tiandituService.reverseGeocode(dev.getLongitude(), dev.getLatitude());
        String location = "经度:" + dev.getLongitude() + " | 纬度:" + dev.getLatitude();
        dto = reverseLocation(dev, dto, location);
        dto.setTemperature(dev.getTemperature());
        dto.setReportTime(dev.getReportTime());
        return dto;
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
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (device == null) {
            return Result.error("设备不存在");
        }

        DisposalRecord dr = new DisposalRecord();
        dr.setDeviceId(deviceId);
        dr.setG09Onoff(paramSettings.getG09OnOff());
        dr.setG16Onoff(paramSettings.getG16OnOff());
        dr.setG24Onoff(paramSettings.getG24OnOff());
        dr.setG52Onoff(paramSettings.getG52OnOff());
        dr.setG58Onoff(paramSettings.getG58OnOff());
        dr.setDuration(paramSettings.getDuration());
        disposalRecordMapper.insert(dr);
        DeviceCommand command = new DeviceCommand(deviceId, paramSettings);
        // todo 将设备修改信息，持续时间通过MQTT发送给硬件
        try {
            String payload = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .writeValueAsString(command);
            String topic = SystemConstants.TOPIC;
            mqttService.publish(topic, payload);
            unattendedService.logSystemEvent(
                    user,
                    DEVICE_DISPOSAL_EVENT,
                    String.format("MQTT指令已发送 | 设备:%s | payload:%s", deviceId, payload)
            );
        } catch (Exception e) {
            log.error(String.valueOf(e));
            unattendedService.logSystemEvent(
                    user,
                    DEVICE_DISPOSAL_EVENT,
                    String.format("MQTT指令发送失败 | 设备:%s", deviceId)
            );
            return Result.error("更新失败");
        }
        Instant start = Instant.now();
        double duration = paramSettings.getDuration();
        if (duration > 60){
            duration = 60;
        }
        Instant end = start.plus(Duration.ofSeconds((long) duration));
        deviceDisposalManager.startDisposal(device, start, end);
        return Result.success("设备处置已启动");
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
                    r.getG24Onoff() == 1, r.getG52Onoff() == 1, r.getG58Onoff() == 1);
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
            String location = "位置";
            dto.setReportTime(d.getReportTime());
            dto.setTemperature(d.getTemperature());
            dto = reverseLocation(d,dto,location);

            return dto;
        }).collect(Collectors.toList());


        return Result.success(dtos);
    }



    private DeviceDTO reverseLocation(Device d, DeviceDTO dto, String location) {
        // 如果没有经纬度，保留默认提示
        if (d.getLatitude() == null || d.getLongitude() == null) {
            dto.setLocation(location);
            return dto;
        }

        String deviceKey = d.getId();


        // 先尝试从缓存取地址（非阻塞读取）
        Optional<GeoEntry> maybe = geoCacheService.getEntry(deviceKey);
        if (maybe.isPresent() && maybe.get().getAddress() != null) {
            // 缓存命中并且已经解析出地址
            dto.setLocation(maybe.get().getAddress());
        } else {
            // 缓存未命中或地址尚未解析：显示经纬度占位，同时异步触发解析（updateLocation 要是幂等/去重的）
            location = "经度：" + d.getLongitude() + "  纬度：" + d.getLatitude();
            dto.setLocation(location);

            // 异步触发解析（不会阻塞当前请求）
            geoCacheService.updateLocation(deviceKey, d.getLongitude(), d.getLatitude());
        }
        return dto;

    }


}




