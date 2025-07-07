package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.alarm.AlarmQuery;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 28611
* @description 针对表【alarm(告警信息表)】的数据库操作Service
* @createDate 2025-07-07 09:44:52
*/
public interface AlarmService extends IService<Alarm> {

    Result<?> listAlarms(AlarmQuery query);

    Result<?> updateAlarm(Long alarmId, AlarmUpdateReq req) throws BusinessException;

    Result<?> deleteAlarm(Long alarmId) throws BusinessException;

    Result<?> batchDelete(List<String> ids);
}
