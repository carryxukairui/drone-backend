package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.alarm.AlarmQueryReq;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.dto.screen.RealtimeAlarmReq;
import com.demo.dronebackend.dto.screen.FlightHistoryQuery;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 告警信息Service
 */
public interface AlarmService extends IService<Alarm> {

    /**
     * 硬件请求处理
     */
    Result<?> handleDroneReport(DroneReport report);

    /**
     * 驾驶舱-实时告警
     */
    Result<?> realtimeAlarms(RealtimeAlarmReq req);


    /**
     * 系统管理-感知记录管理
     */
    Result<?> listAlarms(AlarmQueryReq req);

    Result<?> updateAlarm(Long alarmId, AlarmUpdateReq req) throws BusinessException;

    Result<?> deleteAlarm(Long alarmId) throws BusinessException;

    Result<?> batchDelete(List<Long> ids);

    Result<?> historyList(FlightHistoryQuery query);

    Result<?> getHourlyDistribution();

    Result<?> getWeeklyDistribution();

    Result<?> getMonthlyDistribution();

    Result<?> getYearDistribution();

    Result<?> getAllDroneDistribution();

}
