package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.pojo.Drone;
import com.demo.dronebackend.service.DroneService;
import com.demo.dronebackend.mapper.DroneMapper;
import org.springframework.stereotype.Service;

/**
* @author 28611
* @description 针对表【drone(无人机名单表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
public class DroneServiceImpl extends ServiceImpl<DroneMapper, Drone>
    implements DroneService{

}




