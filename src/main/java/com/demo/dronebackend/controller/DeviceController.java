package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.device.DeviceCommand;
import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.dto.disposal.BatchDeleteRequest;
import com.demo.dronebackend.dto.hardware.StatusReport;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.service.MqttService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("admin/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;
    private final MqttService mqttService;
    private static final String topic = "device/command/startJam";


    /*
     * 添加设备
     */
    @PostMapping()
    public Result<?> addDevice(@Valid  @RequestBody DeviceReq req) {

        return deviceService.addDevice(req);
    }

    /*
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteDevice(@PathVariable String id) {
        return deviceService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除设备
     * @param req
     * @return
     */
    @PostMapping("/batch_delete")
    public Result<?> deleteBatch(
            @Valid @RequestBody BatchDeleteRequest<Long> req) {
        return deviceService.deleteBatch(req.getIds());
    }


    /*
     * 修改设备信息
     */
    @PutMapping("/{id}")
    public Result<?> updateDevice(
            @PathVariable("id") String pathId,
            @Valid @RequestBody DeviceReq req) {
        if (!pathId.equals(req.getId())) {
            return (Result.error("URL 中的设备 ID 与请求体不一致"));
        }
        return deviceService.updateDevice(req) ;
    }


    /*
    获取设备列表，可分页；用户是自己的设备，管理员是所有人的设备
     */
    @GetMapping()
    public Result<?> getDevices( @ModelAttribute DeviceQuery query) {

        return deviceService.listDevices(query);
    }
    @PostMapping("/publish")
    public String publish(@RequestBody DeviceCommand  command) throws Exception {
        //把command转化为json字符串
        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(command);
        mqttService.publish(topic, message);
        return "Published to topic: " + topic+" : "+message;
    }



    /**
     * 订阅设备状态
     */
    @PostMapping("/sub")
    @CrossOrigin
    public Map<String, Object> sub(@RequestBody StatusReport statusReport) {
        return deviceService.websocketDevice(statusReport);
    }


}
