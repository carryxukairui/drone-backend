package com.demo.dronebackend.mapper;

import com.demo.dronebackend.pojo.Drone;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author 28611
* @description 针对表【drone(无人机名单表)】的数据库操作Mapper
* @createDate 2025-07-07 09:44:52
* @Entity com.demo.dronebackend.pojo.Drone
*/
public interface DroneMapper extends BaseMapper<Drone> {
    @Select("SELECT type FROM drone WHERE drone_sn = #{sn} LIMIT 1")
    String findTypeBySn(@Param("sn") String sn);
}




