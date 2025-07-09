package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.alarm.AlarmDto;
import com.demo.dronebackend.dto.alarm.AlarmQuery;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.dto.screen.FlightHistoryDto;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.mapper.AlarmMapper;
import com.demo.dronebackend.util.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
* @author 28611
* @description 针对表【alarm(告警信息表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, Alarm>
    implements AlarmService{

    private final AlarmMapper alarmMapper;


    @Override
    public Result<?> listAlarms(AlarmQuery q) {
        Page<Alarm> page = new Page<>(q.getPage(), q.getSize());

        LambdaQueryWrapper<Alarm> qw = new LambdaQueryWrapper<>();
        if (q.getDroneId() != null) {
            qw.eq(Alarm::getDroneId, q.getDroneId());
        }
        if (StringUtils.hasText(q.getDroneModel())) {
            qw.like(Alarm::getDroneModel, q.getDroneModel());
        }
        if (q.getStartTime() != null) {
            qw.ge(Alarm::getTakeoffTime, q.getStartTime());
        }
        if (q.getEndTime() != null) {
            qw.le(Alarm::getLandingTime, q.getEndTime());
        }
        if (q.getStationId() != null) {
            qw.eq(Alarm::getStationId, q.getStationId());
        }
        if (q.getDetectType() != null) {
            qw.eq(Alarm::getDetectType, q.getDetectType());
        }

        User me = CurrentUserContext.get();
        if(PermissionType.admin.getDesc().equals( me.getPermission())){
            Long userId = me.getId();
            qw.inSql(Alarm::getScanid,
                    "SELECT id FROM device WHERE device_user_id = " + userId);
        }

        Page<Alarm> alarmPage = alarmMapper.selectPage(page, qw);
        List<AlarmDto> dtoList = alarmPage.getRecords().stream().map(a -> {
            AlarmDto dto = new AlarmDto();
            dto.setId(String.valueOf(a.getId()));
            dto.setDroneModel(a.getDroneModel());
            dto.setIntrusionTime(a.getIntrusionStartTime());
            // TODO:接入对应的位置信息api接口获取
            dto.setLocation("地址");

            dto.setType(a.getDroneType());
            dto.setDroneSn(a.getDroneSn());
            return dto;
        }).toList();

        MyPage<AlarmDto> resultPage = new MyPage<>(
                alarmPage.getCurrent(),
                alarmPage.getPages(),
                alarmPage.getSize(),
                alarmPage.getTotal(),
                dtoList
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

        //TODO:解析位置信息 转为坐标
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
    public Result<?> batchDelete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除失败：ID 列表为空");
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
            qw.eq(Alarm::getDroneId, q.getDroneId());
        }
        if (StringUtils.hasText(q.getDroneSn())) {
            qw.eq(Alarm::getDroneSn, q.getDroneSn());
        }
        if (StringUtils.hasText(q.getModel())) {
            qw.eq(Alarm::getDroneModel, q.getModel());
        }
        if (q.getDroneType() != null) {
            qw.eq(Alarm::getDroneType, q.getDroneType());
        }
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
            //TODO:
            dto.setDroneType( "国标");
            dto.setFrequency(r.getFrequency());
            dto.setLastingTime(r.getLastingTime());
            //TODO:
            dto.setDisposal(true);
            dto.setPilotLongitude(r.getPilotLongitude());
            dto.setPilotLatitude(r.getPilotLatitude());
            //TODO:对应的经纬度
            dto.setTakeoffLongitude(1.11);
            dto.setTakeoffLatitude(1.11);

            dto.setLastLongitude(r.getLastLongitude());
            dto.setLastLatitude(r.getLastLatitude());
            return dto;
        }).toList();

        return Result.success(new MyPage<>(
                pr.getCurrent(), pr.getPages(),
                pr.getSize(), pr.getTotal(), dtoList));
    }
}




