package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Drone;
import com.demo.dronebackend.service.DroneService;
import com.demo.dronebackend.mapper.DroneMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
* @author 28611
* @description 针对表【drone(无人机名单表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
public class DroneServiceImpl extends ServiceImpl<DroneMapper, Drone> implements DroneService{
    @Override
    public Result<Page<Drone>> getDroneList(Integer currentPage,Integer pageSize,String droneBrand,String droneModel,String droneSn,String type,Long userId) {
        Page<Drone> page = new Page<>(currentPage,pageSize);
        LambdaQueryWrapper<Drone> queryWrapper = new LambdaQueryWrapper<>();
        //条件非空判断
        if(droneBrand != null&& !droneBrand.isEmpty()){
            queryWrapper.like(Drone::getDroneBrand,droneBrand);
        }
        if(droneModel != null&& !droneModel.isEmpty()){
            queryWrapper.like(Drone::getDroneModel,droneModel);
        }
        if(droneSn != null&& !droneSn.isEmpty()){
            queryWrapper.like(Drone::getDroneSn,droneSn);
        }
        if(type != null&& !type.isEmpty()){
            queryWrapper.like(Drone::getType,type);
        }
        if(userId != null){
            queryWrapper.eq(Drone::getUserId,userId);
        }
        Page<Drone> pageData = this.page(page,queryWrapper);
        return  Result.success(pageData);
    }

    @Override
    public Result<Drone> getDroneById(Long id) {
        Drone drone = this.getById(id);
        return Result.success(drone);
    }

    @Override
    public Result<Drone> addDrone(Drone drone) {
        boolean save = this.save(drone);
        return Result.success(drone);
    }

    @Override
    public Result<Drone> updateDrone(Drone drone) {
        boolean update = this.updateById(drone);
        return Result.success(drone);
    }

    @Override
    public Result deleteDrone(Long id) {
        boolean delete = this.removeById(id);
        return Result.success(id);
    }

    @Override
    public Result deleteDroneBatch(Long[] ids) {
        boolean delete = this.removeByIds(Arrays.asList(ids));
        return Result.success(ids);
    }
}




