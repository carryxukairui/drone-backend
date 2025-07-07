package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.pojo.Device;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.mapper.DeviceMapper;
import org.springframework.stereotype.Service;

/**
* @author 28611
* @description 针对表【device(设备表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device>
    implements DeviceService{

}




