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
    public Result<?> getAlarms(@Valid @ModelAttribute AlarmQueryReq req) {
        return alarmService.listAlarms(req);



    /*
    * 更新告警信息不需要这部分，不能更新告警信息
     */
    @PutMapping("/{alarm_id}")
    public  Result<?> updateAlarm(
            @PathVariable("alarm_id") Long alarmId,
            @Valid @RequestBody AlarmUpdateReq req) {
        return  alarmService.updateAlarm(alarmId, req);
    }

    /**
     * 删除告警信息
     * @param alarmId 告警id
     * @return
     */

    @DeleteMapping("/{alarm_id}")
    public Result<?> deleteAlarm(@PathVariable("alarm_id") Long alarmId)  {
        return alarmService.deleteAlarm(alarmId);
    }


    /**
     * 批量删除告警信息
     * @RequestBody BatchDeleteRequest 删除告警的id列表
     * @return
     */
    @PostMapping("/batch_delete")
    public  Result<?> batchDelete(@Valid @RequestBody BatchDeleteRequest req) {

        return alarmService.batchDelete(req.getIds());
    }

}
