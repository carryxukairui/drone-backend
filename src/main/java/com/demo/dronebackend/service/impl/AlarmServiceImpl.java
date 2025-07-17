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
import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.dto.screen.*;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.DateCount;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.TiandituService;
import com.demo.dronebackend.service.UnattendedService;
import com.demo.dronebackend.util.AlarmConverter;
import com.demo.dronebackend.util.CurrentUserContext;
import com.demo.dronebackend.ws.WebSocketService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.demo.dronebackend.constant.SystemConstants.TRAJECTORY_TIME;

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

    private RealtimeAlarmReq req = new RealtimeAlarmReq();

    @Override
    public Result<?> handleDroneReport(DroneReport report) {
        Alarm alarm = AlarmConverter.fromReport(report);
        this.save(alarm);
        Long userId = deviceMapper.findUserIdsByDeviceId(alarm.getScanid());
        if (userId == null) {
            log.info("告警信息中的设备{}尚未绑定任何用户", alarm.getId());
            return Result.success("无可推送用户", null);
        }
        // 根据用户id获取最新告警集合
        MyPage<RealTimeAlarmDTO> myPage = getRealtimeAlarms(userId);
        // 推送到设备绑定用户
        String topic = SystemConstants.ALARM_WEBSOCKET_TOPIC + ":" + userId;
        webSocketService.sendAlarmListToUser(topic, myPage);

        User user = userMapper.selectById(userId);
        //TODO:判断是否无人值守模块
        if (user != null) {
            unattendedService.onTdoaAlarm(alarm, user);
        }
        return Result.success("推送成功", null);
    }

    @Override
    public Result<?> realtimeAlarms(RealtimeAlarmReq req) {
        if (req != null) {
            this.req = req; // 第一次展示，同步展示条件参数
        }
        Long userId = StpUtil.getLoginIdAsLong();
        MyPage<RealTimeAlarmDTO> myPage = getRealtimeAlarms(userId);
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
        // 按条件查询告警 + 黑白名单类型，只显示未处置记录
        List<Map<String, Object>> rawList = alarmMapper.queryAlarmWithDroneDedup(
                req.getStartTime(),
                req.getEndTime(),
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
            String type = (String)row.get("d_type");
            if (type==null) type="gray";
            dto.setType(type);
            Object intrusionTimeObj = row.get("intrusion_start_time");
            if (intrusionTimeObj != null) {
                dto.setIntrusionTime(intrusionTimeObj instanceof Timestamp
                        ? (Timestamp) intrusionTimeObj
                        : Timestamp.valueOf((LocalDateTime) intrusionTimeObj));
            }
            double lastLongitude = ((Number) row.get("last_longitude")).doubleValue();
            double lastLatitude = ((Number) row.get("last_latitude")).doubleValue();
            // 经纬度转换
            String location = tiandituService.reverseGeocode(lastLongitude, lastLatitude);
            dto.setLocation(location);
            return dto;
        }).toList();
        // 分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allDtos.size());
        List<RealTimeAlarmDTO> pagedList = fromIndex >= allDtos.size() ? Collections.emptyList() : allDtos.subList(fromIndex, toIndex);
        MyPage<RealTimeAlarmDTO> myPage = new MyPage<>();
        myPage.setCurrent(page);
        myPage.setSize(size);
        myPage.setTotal(allDtos.size());
        myPage.setPages((long) Math.ceil((double) allDtos.size() / size));
        myPage.setRecords(pagedList);
        return myPage;
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
        // 计算查询的时间点
        Date startTime = new Date(intrusionStartTime.getTime() - TRAJECTORY_TIME);

        // 查询指定时间内该 droneSn 的所有告警，按入侵时间升序排序
        List<Alarm> recentAlarms = alarmMapper.selectRecentAlarms(droneSn, startTime, intrusionStartTime);
        // 合并轨迹
        List<Map<Object, Object>> mergedTrajectory = new ArrayList<>();

        for (Alarm alarm : recentAlarms) {
            Object rawTrajectory = alarm.getTrajectory();
            if (rawTrajectory == null) continue;
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
                mergedTrajectory.addAll(trajectoryList);
            } catch (Exception e) {
                log.error("解析 trajectory 失败, alarmId = {}", alarm.getId(), e);
            }
        }

        // 构造 DTO
        DetailedAlarmDTO dto = BeanUtil.copyProperties(currentAlarm, DetailedAlarmDTO.class);
        dto.setTrajectory(mergedTrajectory);
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

        MyPage<AlarmDTO> resultPage = new MyPage<>(
                alarmPage.getCurrent(),
                alarmPage.getPages(),
                alarmPage.getSize(),
                alarmPage.getTotal(),
                dtoList,
                null
        );
        return Result.success(resultPage);
    }

    @Override
    public Result<?> updateAlarm(Long alarmId, AlarmUpdateReq req) {
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
        if (StringUtils.hasText(q.getDroneId())) {
            qw.like(Alarm::getDroneId, q.getDroneId());
        }
        if (StringUtils.hasText(q.getDroneSn())) {
            qw.like(Alarm::getDroneSn, q.getDroneSn());
        }
        if (StringUtils.hasText(q.getModel())) {
            qw.eq(Alarm::getDroneModel, q.getModel());
        }
        if (q.getDroneType() != null) {
            qw.eq(Alarm::getDroneType, q.getDroneType());
        }

        //TODO:反制是如何得知的？
        // 已反制/未反制过滤
        if (Boolean.TRUE.equals(q.getDisposalFlag())) {
            qw.inSql(Alarm::getScanid,
                    "SELECT id FROM device WHERE device_user_id = " + userId);
            qw.isNotNull(Alarm::getLastingTime);
        } else if (Boolean.FALSE.equals(q.getDisposalFlag())) {
            // 未反制
            qw.inSql(Alarm::getScanid,
                    "SELECT id FROM device WHERE device_user_id = " + userId);
            qw.isNull(Alarm::getLastingTime);
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
            dto.setDisposal(true);
            dto.setPilotLongitude(r.getPilotLongitude());
            dto.setPilotLatitude(r.getPilotLatitude());
            //TODO:对应的起飞经纬度
            dto.setTakeoffLongitude(1.11);
            dto.setTakeoffLatitude(1.11);

            dto.setLastLongitude(r.getLastLongitude());
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

        List<HourlyDroneStaDTO> raw = alarmMapper.getHourlyDistribution(start, end, userId);

        Map<Integer, Long> map = raw.stream()
                .collect(Collectors.toMap(
                        HourlyDroneStaDTO::getHour,
                        HourlyDroneStaDTO::getCount
                ));

        List<HourlyDroneStaDTO> stats = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            stats.add(new HourlyDroneStaDTO(i, map.getOrDefault(i, 0L)));
        }
        return Result.success(stats);
    }


    @Override
    public Result<?> getWeeklyDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ZoneId zone = ZoneId.systemDefault();

        Date start = Date.from(monday.atStartOfDay(zone).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(zone).toInstant());

        List<DateCount> raw = alarmMapper.getWeeklyCounts(start, end, userId);

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
        System.out.println("monthMap:" + monthMap);

        List<MonthDroneStatsDTO> stats = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            String monthStr = String.format("%02d", m);
            long count = monthMap.getOrDefault(monthStr, 0L);
            stats.add(new MonthDroneStatsDTO(monthStr, count));
        }

        return Result.success(stats);
    }

    @Override
    public Result<?> getYearDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        long count = alarmMapper.getYearDistribution(userId);
        return Result.success(count);
    }

    @Override
    public Result<?> getAllDroneDistribution() {
        long userId = StpUtil.getLoginIdAsLong();
        long count = alarmMapper.getAllDroneDistribution(userId);
        return Result.success(count);
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
        long todayCount = alarmMapper.selectCount(
                new LambdaQueryWrapper<Alarm>()
                        .between(Alarm::getIntrusionStartTime, todayStart, todayEnd)
        );
        long yesterdayCount = alarmMapper.selectCount(
                new LambdaQueryWrapper<Alarm>()
                        .between(Alarm::getIntrusionStartTime, yesterdayStart, yesterdayEnd)
        );
        long lastYearCount = alarmMapper.selectCount(
                new LambdaQueryWrapper<Alarm>()
                        .between(Alarm::getIntrusionStartTime, lastYearStart, lastYearEnd)
        );
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

    @Override
    public Result<?> getBrandCount() {
        List<Map<String, Object>> brandCount = alarmMapper.countFlightByBrand();
        return Result.success(brandCount);
    }

    @Override
    public Result<?> getSortiesByHour() {
        List<Map<String, Object>> raw = alarmMapper.countSortieByHour();
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




