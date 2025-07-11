package com.demo.dronebackend.mapper;

import com.demo.dronebackend.dto.screen.HourlyDroneStaDTO;
import com.demo.dronebackend.dto.screen.MonthDroneStatsDTO;
import com.demo.dronebackend.dto.screen.WeekDroneStatsDTO;
import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.dronebackend.pojo.DateCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
* @author 28611
* @description 针对表【alarm(告警信息表)】的数据库操作Mapper
* @createDate 2025-07-07 09:44:52
* @Entity com.demo.dronebackend.pojo.Alarm
*/
public interface AlarmMapper extends BaseMapper<Alarm> {
    @Select({
            "SELECT HOUR(a.intrusion_start_time) AS hour,",
            "       COUNT(DISTINCT a.drone_sn)      AS count",
            "  FROM alarm a",
            "  JOIN device d",
            "    ON a.scanID = d.id",
            " WHERE a.intrusion_start_time BETWEEN #{start} AND #{end}",
            "   AND d.device_user_id = #{userId}",
            " GROUP BY HOUR(a.intrusion_start_time)"
    })
    List<HourlyDroneStaDTO> getHourlyDistribution(
            @Param("start")  Date start,
            @Param("end")    Date end,
            @Param("userId") Long userId
    );

    @Select({
            "SELECT DATE(a.intrusion_start_time) AS statDate,",
            "       COUNT(DISTINCT a.drone_sn)        AS cnt",
            "  FROM alarm a",
            "  JOIN device d ON a.scanID = d.id",
            " WHERE a.intrusion_start_time BETWEEN #{start} AND #{end}",
            "   AND d.device_user_id = #{userId}",
            " GROUP BY DATE(a.intrusion_start_time)"
    })
    @Results({
            @Result(property = "statDate", column = "statDate"),
            @Result(property = "cnt",      column = "cnt")
    })
    List<DateCount> getWeeklyCounts(
            @Param("start")  Date start,
            @Param("end")    Date end,
            @Param("userId") long userId
    );



    @Select({
            "SELECT MONTH(a.intrusion_start_time) AS month,",
            "       COUNT(DISTINCT a.drone_sn) AS count ",
            "  FROM alarm a",
            "  JOIN device d ON a.scanID = d.id",
            " WHERE YEAR(a.intrusion_start_time) = YEAR(CURDATE())",
            "   AND a.intrusion_start_time IS NOT NULL",
            "   AND d.device_user_id = #{userId}",
            " GROUP BY MONTH(a.intrusion_start_time)"
    })
    @Results({
            @Result(property = "month", column = "month"),
            @Result(property = "count", column = "count")
    })
    List<MonthDroneStatsDTO> countByMonth(@Param("userId") long userId);




    @Select({
            "SELECT COUNT(DISTINCT a.drone_sn) AS cnt",
            "  FROM alarm a",
            "  JOIN device d",
            "    ON a.scanID = d.id",
            " WHERE d.device_user_id = #{userId}"
    })
    long getAllDroneDistribution(@Param("userId") long userId);

    @Select({
            "SELECT COUNT(DISTINCT a.drone_sn)",
            "  FROM alarm a",
            "  JOIN device d ON a.scanID = d.id",
            " WHERE YEAR(a.intrusion_start_time) = YEAR(CURDATE())",
            "   AND d.device_user_id = #{userId}"
    })
    long getYearDistribution(@Param("userId") long userId);
}




