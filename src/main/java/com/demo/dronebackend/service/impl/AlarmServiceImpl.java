package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.constant.SystemConstants;
import com.demo.dronebackend.dto.alarm.AlarmDTO;
import com.demo.dronebackend.dto.alarm.AlarmQueryReq;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.dto.screen.*;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.model.*;
import com.demo.dronebackend.service.*;
import com.demo.dronebackend.util.AlarmKeyGenerator;
import com.demo.dronebackend.util.GeoEntry;
import com.demo.dronebackend.util.MyPage;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.DateCount;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.ws.WebSocketService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static com.demo.dronebackend.constant.SystemConstants.DEFAULT_ALARM_TIME_RANGE;
import static com.demo.dronebackend.constant.SystemLogConstants.DEVICE_DISPOSAL_EVENT;

/**
 * @author 28611
 * @description 针对表【alarm(告警信息表)】的数据库操作Service实现
 * @createDate 2025-07-07 09:44:52
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, Alarm>
        implements AlarmService {

    private final AlarmMapper alarmMapper;
    private final DeviceMapper deviceMapper;
    private final ObjectMapper objectMapper;
    private final TiandituService tiandituService;
    private final WebSocketService webSocketService;
    private final UserMapper userMapper;
    private final UnattendedService unattendedService;
    private final DeviceDisposalManager deviceDisposalManager;
    private final GeoCacheServiceImpl geoCacheService;
    private final AlarmKeyGenerator alarmKeyGenerator;

    private RealtimeAlarmReq req = new RealtimeAlarmReq();

    // 缓存 key: userId_droneSn
    private final ConcurrentHashMap<String, AlarmPushBuffer> alarmPushMap = new ConcurrentHashMap<>();
    // 延迟任务线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final int PUSH_THRESHOLD = 10; // 告警次数达到10次立即推送
    private final long PUSH_DELAY_MS = 1500; // 最长延迟 1.5 秒推送

    @Override
    public void handleDroneReport(AlarmConvertible report) {
        Alarm alarm = report.toAlarm();
        long newId = alarmKeyGenerator.generateNextKey();
        alarm.setId(newId);
        alarmMapper.upsert(alarm);
        Long userId = deviceMapper.findUserIdsByDeviceId(alarm.getScanid());
        if (userId == null) {
            log.info("告警信息中的设备{}尚未绑定任何用户", alarm.getScanid());
            return;
        }
        // TODO:判断是否是无人值守模式
        User user = userMapper.selectById(userId);
        if (user != null && user.getUnattended() == 1) {
            unattendedService.onTdoaAlarm(alarm, user);
            return;
        }

        // 判断该告警是否在处于反制状态的设备的反制区域内
        String deviceId = deviceDisposalManager.isAlarmInDisposingArea(alarm);
        if (deviceId != null) {
            alarm.setIsDisposed(1);
            alarmMapper.updateById(alarm);
            unattendedService.logSystemEvent(
                    user,
                    DEVICE_DISPOSAL_EVENT,
                    String.format("无人机%s已进入设备反制区域 | 告警ID:%d | 设备ID:%s",
                            alarm.getDroneSn(), alarm.getId(), deviceId)
            );
            return;
        }

        // 执行推送流程
        String droneSn = alarm.getDroneSn();
        String key = userId + "_" + droneSn;
        AlarmPushBuffer buffer = alarmPushMap.computeIfAbsent(key, k -> {
            AlarmPushBuffer b = new AlarmPushBuffer();
            b.setUserId(userId);
            b.setDroneSn(droneSn);
            // 启动延迟推送任务
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                pushBufferedAlarm(userId, droneSn);
            }, PUSH_DELAY_MS, TimeUnit.MILLISECONDS);
            b.setFuture(future);
            return b;
        });
        int newCount = buffer.incrCount();
        // 达到推送阈值，提前推送
        if (newCount >= PUSH_THRESHOLD) {
            ScheduledFuture<?> future = buffer.getFuture();
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            pushBufferedAlarm(userId, droneSn);
        }
    }

    private void pushBufferedAlarm(Long userId, String droneSn) {
        String key = userId + "_" + droneSn;
        AlarmPushBuffer buffer = alarmPushMap.remove(key);
        if (buffer == null) return;
        // 根据用户id获取最新告警集合
        MyPage<RealTimeAlarmDTO> myPage = getRealtimeAlarms(userId);
        String topic = SystemConstants.ALARM_WEBSOCKET_TOPIC + ":" + userId;
        webSocketService.sendAlarmListToUser(topic, myPage);
        log.info("推送完成：用户 {}, 无人机 {}, 缓存告警数 {}", userId, droneSn, buffer.getCount());
    }

    @Override
    public Result<?> realtimeAlarms(RealtimeAlarmReq req) {
        if (req != null) {
            this.req=req; // 第一次展示，同步展示条件参数
            if (StrUtil.isBlank(req.getType())){ // 防止前端传来空字符串
                req.setType(null);
            }
        }
        Long userId = StpUtil.getLoginIdAsLong();
        MyPage<RealTimeAlarmDTO> myPage = getRealtimeAlarms(userId);
        myPage.setSocketType("alarm");
        return Result.success(myPage);
    }

    /**
     * 根据 RealtimeAlarmReq + userId 获取告警信息列表并去重
     * @param userId 用户id
     * @return 自定义分页数据
     */
    private MyPage<RealTimeAlarmDTO> getRealtimeAlarms(Long userId) {
        int page = req.getPage();
        int size = req.getSize();
        int sizeLimit = req.getSize_limit();
        Date startTime= req.getStartTime();
        Date endTime= req.getEndTime();
        // 按条件查询告警 + 黑白名单类型，只显示未处置记录
        List<Map<String, Object>> rawList = alarmMapper.queryAlarmWithDroneDedup(
                startTime,
                endTime,
                req.getDroneModel(),
                req.getType(),
                userId,
                sizeLimit
        );
        if (rawList.isEmpty()) {
            return new MyPage<RealTimeAlarmDTO>();
        }
        // 转换成DTO
        List<RealTimeAlarmDTO> allDtos = rawList.stream().map(row -> {
            RealTimeAlarmDTO dto = new RealTimeAlarmDTO();
            dto.setId(((Number) row.get("id")).longValue());
            dto.setDroneModel((String) row.get("drone_model"));
            dto.setDroneSn((String) row.get("drone_sn"));
            dto.setType((String)row.get("d_type"));
            Object intrusionTimeObj = row.get("intrusion_start_time");
            if (intrusionTimeObj != null) {
                dto.setIntrusionTime(intrusionTimeObj instanceof Timestamp
                        ? (Timestamp) intrusionTimeObj
                        : Timestamp.valueOf((LocalDateTime) intrusionTimeObj));
            }
            dto.setLongitude(((Number) row.get("last_longitude")).doubleValue());
            dto.setLatitude(((Number) row.get("last_latitude")).doubleValue());
            dto.setAltitude(((Number) row.get("last_altitude")).doubleValue());
            dto.setSpeed(((Number) row.get("speed")).doubleValue());
            dto.setBack_longitude(((Number) row.get("back_longitude")).doubleValue());
            dto.setBack_latitude(((Number) row.get("back_latitude")).doubleValue());
            return dto;
        }).toList();
        // 分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allDtos.size());
        List<RealTimeAlarmDTO> pagedList = fromIndex >= allDtos.size() ? Collections.emptyList() : allDtos.subList(fromIndex, toIndex);

        // 分页后再调用天地图解析，减少不必要的解析
        pagedList.forEach(dto -> {
            String location = reverseLocation(dto.getDroneSn(), dto.getLongitude(), dto.getLatitude());
            dto.setLocation(location);
        });
        MyPage<RealTimeAlarmDTO> myPage = new MyPage<>();
        myPage.setCurrent(page);
        myPage.setSize(size);
        myPage.setTotal(allDtos.size());
        myPage.setPages((long) Math.ceil((double) allDtos.size() / size));
        myPage.setRecords(pagedList);
        return myPage;
    }

    private String reverseLocation(String alarmKey, double longitude, double latitude) {
        String location = "";
        // 先尝试从缓存取地址（非阻塞读取）
        Optional<GeoEntry> maybe = geoCacheService.getEntry(alarmKey);
        if (maybe.isPresent() && maybe.get().getAddress() != null) {
            // 缓存命中并且已经解析出地址
            location = maybe.get().getAddress();
        } else {
            // 缓存未命中或地址尚未解析：显示经纬度占位，同时异步触发解析（updateLocation 要是幂等/去重的）
            location = "经度：" + longitude + "  纬度：" + latitude;
            // 异步触发解析（不会阻塞当前请求）
            geoCacheService.updateLocation(alarmKey, longitude, latitude);
        }
        return location;
    }

    @Override
    public Result<?> getAlarm(String id) {
        Long alarmId = Long.parseLong(id);
        Alarm currentAlarm = alarmMapper.selectById(alarmId);
        if (currentAlarm == null) {
            throw new BusinessException("告警不存在");
        }
        String droneSn = currentAlarm.getDroneSn();
        Date intrusionStartTime = currentAlarm.getIntrusionStartTime();
        if (intrusionStartTime == null) {
            throw new BusinessException("当前告警缺少 intrusionStartTime");
        }

        // 计算当天的开始和结束时间
        LocalDate alarmDate = intrusionStartTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        Date startTime = toDate(alarmDate.atStartOfDay());
        Date endTime = toDate(alarmDate.atTime(LocalTime.MAX));

        Long userId = StpUtil.getLoginIdAsLong();
        // 查询当天该 droneSn 的所有告警，按入侵时间升序排序
        List<Alarm> recentAlarms = alarmMapper.selectRecentAlarms(droneSn, startTime, endTime, userId);

        // 合并轨迹：优先使用 trajectory 字段，如果不存在则使用告警的经纬度坐标
        List<Map<Object, Object>> mergedTrajectory = new ArrayList<>();

        for (Alarm alarm : recentAlarms) {
            // 首先尝试添加 trajectory 字段中的轨迹点
            Object rawTrajectory = alarm.getTrajectory();
            if (rawTrajectory != null) {
                try {
                    List<Map<Object, Object>> trajectoryList;
                    if (rawTrajectory instanceof String) {
                        // JSON 字符串（可能数据库驱动没有解析）
                        trajectoryList = objectMapper.readValue(
                                (String) rawTrajectory,
                                new TypeReference<List<Map<Object, Object>>>() {
                                }
                        );
                    } else if (rawTrajectory instanceof List) {
                        // 已是 List，尝试转换（兼容 JSON 数组直接转为 ArrayList）
                        trajectoryList = (List<Map<Object, Object>>) rawTrajectory;
                    } else {
                        // 其他情况（如 JSONArray、LinkedHashMap），先序列化再反序列化为目标格式
                        String json = objectMapper.writeValueAsString(rawTrajectory);
                        trajectoryList = objectMapper.readValue(
                                json,
                                new TypeReference<List<Map<Object, Object>>>() {
                                }
                        );
                    }
                    // 过滤掉无效坐标（经纬度为0的点）
                    for (Map<Object, Object> point : trajectoryList) {
                        if (isValidCoordinate(point)) {
                            mergedTrajectory.add(point);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析 trajectory 失败, alarmId = {}", alarm.getId(), e);
                }
            }

            // 添加告警本身的坐标点（如果经纬度存在且有效）
            if (alarm.getLastLongitude() != null && alarm.getLastLatitude() != null) {
                // 过滤掉无效坐标（0, 0）
                if (isValidCoordinate(alarm.getLastLongitude(), alarm.getLastLatitude())) {
                    Map<Object, Object> alarmPoint = new HashMap<>();
                    // 使用 lng/lat 字段名以保持与原始格式一致
                    alarmPoint.put("lng", alarm.getLastLongitude());
                    alarmPoint.put("lat", alarm.getLastLatitude());
                    mergedTrajectory.add(alarmPoint);
                }
            }
        }

        // 构造 DTO
        DetailedAlarmDTO dto = BeanUtil.copyProperties(currentAlarm, DetailedAlarmDTO.class);
        dto.setTrajectory(mergedTrajectory);
        return Result.success(dto);
    }

    @Override
    public Result<?> disposeDrone(String id) {
        long alarmId = Long.parseLong(id);
        Alarm alarm = alarmMapper.selectById(alarmId);
        long userId = StpUtil.getLoginIdAsLong();
        // todo: 拦截器获取user
        User user = userMapper.selectById(userId);
        return unattendedService.disposeAlarmManually(alarm, user);
    }

    @Override
    public Result<?> getAlarmStatistics() {
        long userId = StpUtil.getLoginIdAsLong();
        LocalDate today = LocalDate.now();
        Date todayStart = toDate(today.atStartOfDay());
        Date todayEnd = toDate(today.atTime(LocalTime.MAX));
        // 今日告警总数
        long todayAlarmCount = alarmMapper.countAlarmsByTime(userId, todayStart, todayEnd);
        // 告警总数
        long totalAlarmCount = alarmMapper.countAlarmsByTime(userId,null,null);
        // 今日处置总数
        long todayDisposalCount = alarmMapper.countDisposedAlarms(userId, todayStart, todayEnd);
        // 处置总数
        long totalDisposalCount = alarmMapper.countDisposedAlarms(userId, null, null);
        // 构造DTO
        AlarmStatisticsDTO dto = new AlarmStatisticsDTO (
                todayAlarmCount,
                totalAlarmCount,
                todayDisposalCount,
                totalDisposalCount
        );
        return Result.success(dto);
    }

    @Override
    public Result<?> listAlarms(AlarmQueryReq q) {
        Page<Alarm> page = new Page<>(q.getPage(), q.getSize());
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        LambdaQueryWrapper<Alarm> qw = new LambdaQueryWrapper<>();
        if (q.getDroneId() != null) {
            qw.eq(Alarm::getDroneId, q.getDroneId());
        }
        if (StrUtil.isNotBlank(q.getDroneModel())) {
            qw.like(Alarm::getDroneModel, q.getDroneModel());
        }
        if (q.getStartTime() != null) {
            qw.ge(Alarm::getTakeoffTime, df.format(q.getStartTime()));
        }
        if (q.getEndTime() != null) {
            qw.le(Alarm::getLandingTime, df.format(q.getEndTime()));
        }
        if (q.getStationId() != null) {
            qw.eq(Alarm::getStationId, q.getStationId());
        }
        if (q.getDetectType() != null) {
            qw.eq(Alarm::getDetectType, q.getDetectType());
        }

        User me = CurrentUserContext.get();
        if (!PermissionType.admin.getDesc().equals(me.getPermission())) {
            Long userId = me.getId();
            qw.inSql(Alarm::getScanid,
                    "SELECT id FROM device WHERE device_user_id = " + userId);
        }

        Page<Alarm> alarmPage = alarmMapper.selectPage(page, qw);
        List<AlarmDTO> dtoList = alarmPage.getRecords().stream().map(a -> {
            String location = tiandituService.reverseGeocode(a.getLastLongitude(), a.getLastLatitude());

            AlarmDTO dto = new AlarmDTO();
            dto.setId(a.getId());
            dto.setDroneModel(a.getDroneModel());
            dto.setIntrusionTime(a.getIntrusionStartTime());
            dto.setLocation(location);
            dto.setType(a.getDroneType());
            dto.setDroneSn(a.getDroneSn());
            return dto;
        }).toList();

        MyPage<Alarm> resultPage = new MyPage<>(
                alarmPage.getCurrent(),
                alarmPage.getPages(),
                alarmPage.getSize(),
                alarmPage.getTotal(),
                alarmPage.getRecords(),
                null
        );
        return Result.success(resultPage);
    }

    @Override
    public Result<?> updateAlarm(Long alarmId, AlarmUpdateReq req){
        // 1. 查询原告警记录
        Alarm alarm = alarmMapper.selectById(alarmId);
        if (alarm == null) {
            throw new BusinessException("感知记录不存在，ID=" + alarmId);
        }


        if (req.getIntrusionTime() != null) {
            alarm.setIntrusionStartTime(req.getIntrusionTime());
        }
        alarm.setDroneModel(req.getDroneModel());
        alarm.setDroneSn(req.getDroneSn());

        if (req.getType() != null) {
            alarm.setType(req.getType());
        }

        //解析位置信息 转为坐标
        // alarm.setLocation(req.getLocation());

        // 3. 执行更新
        int updated = alarmMapper.updateById(alarm);
        if (updated == 0) {
            throw new BusinessException("修改失败，请稍后重试");
        }
        return Result.success("感知记录修改成功");
    }

    @Override
    public Result<?> deleteAlarm(Long alarmId) {
        int deleted = alarmMapper.deleteById(alarmId);
        if (deleted == 0) {
            throw new BusinessException("感知记录不存在或已删除，ID=" + alarmId);
        }
        return Result.success("感知记录删除成功");
    }

    @Override
    public Result<?> batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除失败：ID 列表为空");
        }
        User user = CurrentUserContext.get();
        if (!PermissionType.admin.getDesc().equals(user.getPermission())) {
            throw new BusinessException("权限不足");
        }
        int deleted = alarmMapper.deleteBatchIds(ids);
        if (deleted == 0) {
            throw new BusinessException("未删除任何记录，请检查 ID 是否正确");
        }
        return Result.success("批量删除成功");
    }

    @Override
    public Result<?> historyList(FlightHistoryQuery q) {
        Page<Alarm> page = new Page<>(q.getPage(), q.getSize());
        LambdaQueryWrapper<Alarm> qw = new LambdaQueryWrapper<>();

        long userId = StpUtil.getLoginIdAsLong();

        if (q.getStartTime() != null) {
            qw.ge(Alarm::getTakeoffTime, q.getStartTime());
        }
        if (q.getEndTime() != null) {
            qw.le(Alarm::getTakeoffTime, q.getEndTime());
        }
        if (StrUtil.isNotBlank(q.getDroneId())) {
            qw.like(Alarm::getDroneId, q.getDroneId());
        }
        if (StrUtil.isNotBlank(q.getDroneSn())) {
            qw.like(Alarm::getDroneSn, q.getDroneSn());
        }
        if (StrUtil.isNotBlank(q.getModel())) {
            qw.eq(Alarm::getDroneModel, q.getModel());
        }
        if (StrUtil.isNotBlank(q.getDroneType())) {
            qw.eq(Alarm::getDroneType, q.getDroneType());
        }

        //TODO:反制是如何得知的？
        // 已反制/未反制过滤
        if (Boolean.TRUE.equals(q.getDisposalFlag())) {
            qw.eq(Alarm::getIsDisposed,1);

        } else if (Boolean.FALSE.equals(q.getDisposalFlag())) {
            // 未反制
            qw.eq(Alarm::getIsDisposed,0);
        }

        Page<Alarm> pr = alarmMapper.selectPage(page, qw);

        var dtoList = pr.getRecords().stream().map(r -> {
            FlightHistoryDto dto = new FlightHistoryDto();
            dto.setTakeoffTime(r.getTakeoffTime());
            dto.setLandingTime(r.getLandingTime());
            dto.setDroneId(r.getDroneId());
            dto.setDroneSn(r.getDroneSn());
            dto.setModel(r.getDroneModel());
            dto.setDroneType(r.getDroneType());
            dto.setFrequency(r.getFrequency());
            dto.setLastingTime(r.getLastingTime());
            //TODO:反制
            dto.setDisposal(r.getIsDisposed());
            dto.setPilotLongitude(r.getBackLongitude());
            dto.setPilotLatitude(r.getLastLatitude());

            dto.setTakeoffLongitude(r.getLastLongitude());
            dto.setTakeoffLatitude(r.getLastLatitude());
            dto.setLastLongitude(r.getBackLongitude());
            dto.setLastLatitude(r.getLastLatitude());
            return dto;
        }).toList();

        return Result.success(new MyPage<>(
                pr.getCurrent(), pr.getPages(),
                pr.getSize(), pr.getTotal(), dtoList,null
        ));
    }

    @Override
    public Result<?> getHourlyDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Date start = Date.from(today.atStartOfDay(zone).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());

        List<HourlyDroneStaDTO> raw = alarmMapper.getHourlyDistribution(start, end,userId);

        Map<Integer,Long> map = raw.stream()
                .collect(Collectors.toMap(
                        HourlyDroneStaDTO::getHour,
                        HourlyDroneStaDTO::getCount
                ));

        List<HourlyDroneStaDTO> stats = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            stats.add(new HourlyDroneStaDTO(i, map.getOrDefault(i, 0L)));
        }
        return Result.success( stats);
    }


    @Override
    public Result<?> getWeeklyDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ZoneId zone = ZoneId.systemDefault();

        Date start = Date.from(monday.atStartOfDay(zone).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());

        List<DateCount> raw = alarmMapper.getWeeklyCounts(start, end,userId);

        // 转换结果时直接使用 getter 方法
        Map<LocalDate, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        dc -> dc.getStatDate().toInstant().atZone(zone).toLocalDate(),
                        DateCount::getCnt
                ));

        List<WeekDroneStatsDTO> stats = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            String dayCode = String.format("%02d", date.getDayOfWeek().getValue());

            long cnt = !date.isAfter(today)
                    ? map.getOrDefault(date, 0L)
                    : 0L;

            stats.add(new WeekDroneStatsDTO(dayCode, cnt));
        }

        return Result.success(stats);
    }


    @Override
    public Result<?> getMonthlyDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        List<MonthDroneStatsDTO> raw = alarmMapper.countByMonth(userId);

        Map<String, Long> monthMap = raw.stream()
                .collect(Collectors.toMap(
                        MonthDroneStatsDTO::getMonth,
                        MonthDroneStatsDTO::getCount
                ));
        List<MonthDroneStatsDTO> stats = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            String monthStr = String.format("%02d", m);
            long count;
            if (m<10){
                count = monthMap.getOrDefault(String.valueOf(m), 0L);
            }else {
                count = monthMap.getOrDefault(monthStr, 0L);
            }
            stats.add(new MonthDroneStatsDTO(monthStr, count));
        }
        return Result.success(stats);
    }

    @Override
    public Result<?> getYearDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        long count = alarmMapper.getYearDistribution(userId);
        return Result.success( count);
    }

    @Override
    public Result<?> getAllDroneDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        long count = alarmMapper.getAllDroneDistribution(userId);
        return Result.success( count);
    }


    @Override
    public Result<?> getMonitorCount() {
        // 今天、昨天、去年今天
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastYearToday = today.minusYears(1);

        Date todayStart = toDate(today.atStartOfDay());
        Date todayEnd = toDate(today.atTime(LocalTime.MAX));

        Date yesterdayStart = toDate(yesterday.atStartOfDay());
        Date yesterdayEnd = toDate(yesterday.atTime(LocalTime.MAX));

        Date lastYearStart = toDate(lastYearToday.atStartOfDay());
        Date lastYearEnd = toDate(lastYearToday.atTime(LocalTime.MAX));

        // 查询各时间段数据量
        Long userId = StpUtil.getLoginIdAsLong();
        long todayCount = alarmMapper.countAlarmsByTime(userId, todayStart, todayEnd);
        long yesterdayCount = alarmMapper.countAlarmsByTime(userId, yesterdayStart, yesterdayEnd);
        long lastYearCount = alarmMapper.countAlarmsByTime(userId, lastYearStart, lastYearEnd);
        // 构造返回值
        return Result.success(new MonitorCountDTO(todayCount,calcRate(todayCount, lastYearCount),calcRate(todayCount, yesterdayCount)));
    }

    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 增长率计算
     * @param current 当前值（今天）
     * @param compare 对比值（昨天或去年）
     */
    private String calcRate(long current, long compare) {
        if (compare == 0) {
            if (current == 0) {
                return "0%";
            } else {
                return "100%+";// 对比值为0，新增长
            }
        }
        double rate = (current - compare) * 100.0 / compare;
        return String.format("%.2f%%", rate);
    }

    /**
     * 验证坐标点是否有效（过滤掉经纬度为0的无效点）
     * @param point 包含经纬度信息的Map
     * @return 坐标是否有效
     */
    private boolean isValidCoordinate(Map<Object, Object> point) {
        if (point == null) {
            return false;
        }
        // 兼容两种字段名格式：lng/lat 和 longitude/latitude
        Object lonObj = point.get("lng");
        if (lonObj == null) {
            lonObj = point.get("longitude");
        }

        Object latObj = point.get("lat");
        if (latObj == null) {
            latObj = point.get("latitude");
        }

        if (lonObj == null || latObj == null) {
            return false;
        }

        try {
            double longitude = ((Number) lonObj).doubleValue();
            double latitude = ((Number) latObj).doubleValue();
            return isValidCoordinate(longitude, latitude);
        } catch (Exception e) {
            log.warn("坐标格式错误: {}", point);
            return false;
        }
    }

    /**
     * 验证经纬度是否有效（过滤掉0,0坐标点）
     * @param longitude 经度
     * @param latitude 纬度
     * @return 坐标是否有效
     */
    private boolean isValidCoordinate(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            return false;
        }
        // 过滤掉 (0, 0) 坐标点，通常表示无效的GPS数据
        // 使用小的容差值来处理浮点数比较
        return Math.abs(longitude) > 0.0001 || Math.abs(latitude) > 0.0001;
    }

    @Override
    public Result<?> getBrandCount() {
        LocalDate today = LocalDate.now();
        Date startTime = toDate(today.atStartOfDay());
        Date endTime = toDate(today.atTime(LocalTime.MAX));
        // 获取品牌和对应起飞架次
        List<Map<String, Object>> data = alarmMapper.countFlightByBrand(StpUtil.getLoginIdAsLong(), startTime, endTime);
        // 计算总架次
        long total = data.stream()
                .mapToLong(m -> ((Number) m.get("sortie_count")).longValue())
                .sum();
        // 按数值计算占比
        data.forEach(m -> {
            long count = ((Number) m.get("sortie_count")).longValue();
            double percentage = total == 0 ? 0 : (count * 100.0 / total);
            m.put("percentage", percentage);
        });
        // 按占比降序排序
        data.sort((m1, m2) -> {
            double p1 = ((Number) m1.get("percentage")).doubleValue();
            double p2 = ((Number) m2.get("percentage")).doubleValue();
            return Double.compare(p2, p1);
        });
        // 格式化成字符串
        data.forEach(m -> {
            double percentage = ((Number) m.get("percentage")).doubleValue();
            m.put("percentage", String.format("%.2f%%", percentage));
        });
        return Result.success(data);
    }

    @Override
    public Result<?> getSortiesByHour() {
        List<Map<String, Object>> raw = alarmMapper.countSortieByHour(StpUtil.getLoginIdAsLong());
        // 转换成 Map<String, Integer>，方便补全
        Map<String, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : raw) {
            String hourStr = (String) row.get("hourStr");
            Integer count = ((Number) row.get("sortieCount")).intValue();
            countMap.put(hourStr, count);
        }
        // 补全 00:00 ~ 23:00 每小时
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            String hour = String.format("%02d", i);// 补零
            String timeLabel = hour + ":00";// 拼接，如 "01:00"
            Integer count = countMap.getOrDefault(hour, 0);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("time", timeLabel);
            entry.put("sortie_count", count);
            result.add(entry);
        }
        return Result.success(result);
    }
}




