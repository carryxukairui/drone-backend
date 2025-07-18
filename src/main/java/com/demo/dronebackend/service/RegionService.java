package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.screen.RegionReq;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Region;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 28611
* @description 针对表【region(用户自定义区域表（预警区/反制区/核心区）)】的数据库操作Service
* @createDate 2025-07-14 13:54:35
*/
public interface RegionService extends IService<Region> {

    Result<?> createAlertRegion(RegionReq req);


    Result<?> getAlertRegion();

    Result<?> deleteAlertRegion(String id);
}
