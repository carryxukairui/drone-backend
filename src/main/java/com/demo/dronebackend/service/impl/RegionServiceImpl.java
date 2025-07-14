package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.screen.RegionReq;
import com.demo.dronebackend.mapper.RegionMapper;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.Region;
import com.demo.dronebackend.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 28611
* @description 针对表【region(用户自定义区域表（预警区/反制区/核心区）)】的数据库操作Service实现
* @createDate 2025-07-14 13:54:34
*/
@Service
@RequiredArgsConstructor
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region>
    implements RegionService{

    private final RegionMapper regionMapper;
    @Override
    public Result<?> createAlertRegion( RegionReq req) {
        long userId = StpUtil.getLoginIdAsLong();

        Region region = new Region();
        Date time = new Date();

        region.setTime(time);
        region.setUserId(userId);
        region.setCenterLon(req.getCenterLon());
        region.setCenterLat(req.getCenterLat());
        region.setRadius(req.getRadius());
        region.setType( req.getAlertType());
        regionMapper.insert(region);
        return Result.success("创建成功");
    }


}




