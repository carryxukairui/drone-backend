package com.demo.dronebackend.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.dronebackend.dto.alarm.AlarmQuery;
import com.demo.dronebackend.dto.alarm.AlarmUpdateReq;
import com.demo.dronebackend.dto.alarm.BatchDeleteRequest;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/alarms")
@RequiredArgsConstructor
public class AlarmController {
    private final AlarmService alarmService;


    @GetMapping()
    public Result<?> getAlarms(@Valid AlarmQuery query) {
    //TODO:判断管理员和用户
        return alarmService.listAlarms(query);
    }

    @PutMapping("/{alarm_id}")
    public  Result<?> updateAlarm(
            @PathVariable("alarm_id") Long alarmId,
            @Valid @RequestBody AlarmUpdateReq req) {
        return  alarmService.updateAlarm(alarmId, req);
    }

    @DeleteMapping("/{alarm_id}")
    public Result<?> deleteAlarm(@PathVariable("alarm_id") Long alarmId)  {
        return alarmService.deleteAlarm(alarmId);
    }

    @PostMapping("/batch_delete")
    public  Result<?> batchDelete(@Valid @RequestBody BatchDeleteRequest req) {

        return alarmService.batchDelete(req.getIds());
    }

}
