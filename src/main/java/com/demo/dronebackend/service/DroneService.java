package com.demo.dronebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.pojo.Drone;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 28611
* @description 针对表【drone(无人机名单表)】的数据库操作Service
* @createDate 2025-07-07 09:44:52
*/
public interface DroneService extends IService<Drone> {

   Result<Page<Drone>> getDroneList(Integer currentPage,Integer pageSize,String droneBrand,String droneModel,String droneSn,String type);
   Result<Drone> getDroneById(Long id);
   Result<Drone> addDrone(Drone drone);
   Result<Drone> updateDrone(Drone drone);
   Result deleteDrone(Long id);
   Result deleteDroneBatch(Long[] ids);
}
