package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.device.DeviceQuery;
import com.demo.dronebackend.dto.device.DeviceReq;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;


    @PostMapping("/add")
    public Result<?> addDevice(@Valid  @RequestBody DeviceReq req) {

        return deviceService.addDevice(req);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteDevice(@PathVariable String id) {
        return deviceService.removeById(id) ? Result.success("删除成功") : Result.error("删除失败");
    }

    @PutMapping("/{id}")
    public Result<?> updateDevice(
            @PathVariable("id") String pathId,
            @Valid @RequestBody DeviceReq req) {
        if (!pathId.equals(req.getId())) {
            return (Result.error("URL 中的设备 ID 与请求体不一致"));
        }
        return deviceService.updateDevice(req) ;
    }

    @GetMapping()
    public Result<?> getDevices( @Valid DeviceQuery query) {

        //TODO:需要区分管理员和用户

        return deviceService.listDevices(query);
    }

}
