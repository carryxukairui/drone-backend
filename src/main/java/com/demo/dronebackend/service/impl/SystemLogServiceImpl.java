package com.demo.dronebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.pojo.SystemLog;
import com.demo.dronebackend.service.SystemLogService;
import com.demo.dronebackend.mapper.SystemLogMapper;
import org.springframework.stereotype.Service;

/**
* @author 28611
* @description 针对表【system_log(系统操作日志表)】的数据库操作Service实现
* @createDate 2025-07-17 15:28:28
*/
@Service
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog>
    implements SystemLogService{

}




