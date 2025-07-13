package com.demo.dronebackend.mapper;

import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
* @author 28611
* @description 针对表【alarm(告警信息表)】的数据库操作Mapper
* @createDate 2025-07-07 09:44:52
* @Entity com.demo.dronebackend.pojo.Alarm
*/
@Mapper
public interface AlarmMapper extends BaseMapper<Alarm> {
    @Select("SELECT a.*, d.type AS d_type " +
            "FROM alarm a " +
            "INNER JOIN device dev ON a.scanID = dev.id AND dev.device_user_id = #{userId} " +
            "LEFT JOIN drone d ON a.drone_sn = d.drone_sn AND d.user_id = #{userId} " +
            "WHERE (#{startTime} IS NULL OR a.intrusion_start_time >= #{startTime}) " +
            "AND (#{endTime} IS NULL OR a.intrusion_start_time <= #{endTime}) " +
            "AND (#{droneModel} IS NULL OR a.drone_model LIKE CONCAT('%', #{droneModel}, '%')) " +
            "AND (#{type} IS NULL OR d.type = #{type}) " +
            "ORDER BY a.intrusion_start_time DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> queryAlarmWithDroneDedup(@Param("startTime") Date startTime,
                                                       @Param("endTime") Date endTime,
                                                       @Param("droneModel") String droneModel,
                                                       @Param("type") String type,
                                                       @Param("userId") Long userId,
                                                       @Param("limit") int limit);



}




