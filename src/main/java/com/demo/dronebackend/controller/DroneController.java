package com.demo.dronebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Drone;
import com.demo.dronebackend.service.DroneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drones")
@RequiredArgsConstructor
public class DroneController {
    private final DroneService droneService;

    @GetMapping
    public Result<Page<Drone>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15")  Integer size,
            @RequestParam(required = false)  String droneBrand,
            @RequestParam(required = false)  String droneModel,
            @RequestParam(required = false)  String droneSn,
            @RequestParam(required = false)  String type,
            @RequestParam(required = false)   Long userId
            ) {
        return droneService.getDroneList(page,size,droneBrand,droneModel,droneSn,type,userId);
    }
    @PostMapping
    public Result<Drone> add(@RequestBody Drone drone) {
        return droneService.addDrone(drone);
    }
    @PutMapping
    public Result<Drone> update(@RequestBody Drone drone) {
        return droneService.updateDrone(drone);
    }
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return droneService.deleteDrone(id);
    }
    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody Long[] ids) {
        return droneService.deleteDroneBatch(ids);
    }

}
