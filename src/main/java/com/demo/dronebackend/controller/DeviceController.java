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
    public Result<?> getDevices( @Valid DeviceQuery query) {

        return deviceService.listDevices(query);
    }

}
