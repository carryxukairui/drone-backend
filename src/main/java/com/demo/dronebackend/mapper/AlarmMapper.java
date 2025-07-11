package com.demo.dronebackend.mapper;

import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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
    @Select("SELECT a.*, d.type AS drone_type " +
            "FROM alarm a " +
            "INNER JOIN drone d ON a.drone_sn = d.drone_sn " +
            "WHERE (#{startTime} IS NULL OR a.intrusion_start_time >= #{startTime}) " +
            "AND (#{endTime} IS NULL OR a.intrusion_start_time <= #{endTime}) " +
            "AND (#{droneModel} IS NULL OR a.drone_model LIKE CONCAT('%', #{droneModel}, '%')) " +
            "AND (#{type} IS NULL OR d.type = #{type}) " +
            "ORDER BY a.intrusion_start_time DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> queryAlarmWithDroneDedup(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       @Param("droneModel") String droneModel,
                                                       @Param("type") String type,
                                                       @Param("limit") int limit);
}




