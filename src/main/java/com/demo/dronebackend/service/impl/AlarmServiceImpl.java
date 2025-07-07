package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.pojo.Alarm;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.mapper.AlarmMapper;
import org.springframework.stereotype.Service;

/**
* @author 28611
* @description 针对表【alarm(告警信息表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
public class AlarmServiceImpl extends ServiceImpl<AlarmMapper, Alarm>
    implements AlarmService{

}




