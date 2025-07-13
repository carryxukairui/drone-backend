package com.demo.dronebackend.mapper;

import com.demo.dronebackend.pojo.Device;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 28611
* @description 针对表【device(设备表)】的数据库操作Mapper
* @createDate 2025-07-07 09:44:52
* @Entity com.demo.dronebackend.pojo.Device
*/
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
    @Select("SELECT device_user_id FROM device WHERE id = #{deviceId} LIMIT 1")
    Long findUserIdsByDeviceId(@Param("deviceId") String deviceId);

}




