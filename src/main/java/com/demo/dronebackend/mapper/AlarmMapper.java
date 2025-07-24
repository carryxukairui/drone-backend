package com.demo.dronebackend.mapper;

import com.demo.dronebackend.dto.screen.HourlyDroneStaDTO;
import com.demo.dronebackend.dto.screen.MonthDroneStatsDTO;
import com.demo.dronebackend.pojo.Alarm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.demo.dronebackend.pojo.DateCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * @author 28611
 * @description 针对表【alarm(告警信息表)】的数据库操作Mapper
 * @createDate 2025-07-07 09:44:52
 * @Entity com.demo.dronebackend.pojo.Alarm
 */
@Mapper
public interface AlarmMapper extends BaseMapper<Alarm> {
    @Select("""
                SELECT a.*, COALESCE(d.type, 'gray') AS d_type
                FROM alarm a
                INNER JOIN (
                    SELECT a1.id
                    FROM alarm a1
                    LEFT JOIN alarm a2
                        ON a1.drone_sn = a2.drone_sn
                        AND (
                            a2.intrusion_start_time > a1.intrusion_start_time
                            OR (a2.intrusion_start_time = a1.intrusion_start_time AND a2.id > a1.id)
                        )
                    WHERE a2.id IS NULL AND a1.is_disposed = 0
                ) latest ON a.id = latest.id
                INNER JOIN device dev ON a.scanID = dev.id AND dev.device_user_id = #{userId}
                LEFT JOIN drone d ON a.drone_sn = d.drone_sn AND d.user_id = #{userId}
                WHERE 
                    (#{startTime} IS NULL OR a.intrusion_start_time >= #{startTime})
                    AND (#{endTime} IS NULL OR a.intrusion_start_time <= #{endTime})
                    AND (#{droneModel} IS NULL OR a.drone_model LIKE CONCAT('%', #{droneModel}, '%'))
                    AND (
                        #{type} IS NULL
                        OR (#{type} = 'gray' AND d.drone_sn IS NULL)
                        OR (#{type} != 'gray' AND d.type = #{type})
                    )
                ORDER BY a.intrusion_start_time DESC
                LIMIT #{limit}
            """)
    List<Map<String, Object>> queryAlarmWithDroneDedup(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("droneModel") String droneModel,
            @Param("type") String type,
            @Param("userId") Long userId,
            @Param("limit") int limit
    );


    @Select("""
                SELECT a.* 
                FROM alarm a
                INNER JOIN device dev ON a.scanID = dev.id
                WHERE a.drone_sn = #{droneSn}
                  AND dev.device_user_id = #{userId}
                  AND a.intrusion_start_time BETWEEN #{startTime} AND #{endTime}
                ORDER BY a.intrusion_start_time ASC
            """)
    List<Alarm> selectRecentAlarms(
            @Param("droneSn") String droneSn,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("userId") Long userId
    );


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
            @Param("start") Date start,
            @Param("end") Date end,
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
            @Result(property = "cnt", column = "cnt")
    })
    List<DateCount> getWeeklyCounts(
            @Param("start") Date start,
            @Param("end") Date end,
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


    @Select("""
                SELECT COUNT(*) 
                FROM alarm a
                INNER JOIN device d ON a.scanID = d.id
                WHERE d.device_user_id = #{userId}
                  AND (#{start} IS NULL OR a.intrusion_start_time >= #{start})
                  AND (#{end} IS NULL OR a.intrusion_start_time < #{end})
            """)
    Long countAlarmsByTime(
            @Param("userId") Long userId,
            @Param("start") Date start,
            @Param("end") Date end
    );

    @Select("""
                SELECT COUNT(*)
                FROM alarm a
                INNER JOIN device d ON a.scanID = d.id
                WHERE a.is_disposed = 1
                  AND d.device_user_id = #{userId}
                  AND (#{startTime} IS NULL OR a.intrusion_start_time >= #{startTime})
                  AND (#{endTime} IS NULL OR a.intrusion_start_time < #{endTime})
            """)
    Long countDisposedAlarms(
            @Param("userId") Long userId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime
    );

    @Select("""
                SELECT COUNT(*) AS total
                FROM (
                    SELECT 
                        a.drone_sn,
                        ROW_NUMBER() OVER (PARTITION BY a.drone_sn ORDER BY a.intrusion_start_time DESC) AS rn,
                        a.is_disposed,
                        a.intrusion_start_time
                    FROM alarm a
                    INNER JOIN device dev ON a.scanID = dev.id
                    WHERE dev.device_user_id = #{userId}
                      AND a.intrusion_start_time >= CURDATE()
                      AND a.intrusion_start_time < CURDATE() + INTERVAL 1 DAY
                ) t
                WHERE t.rn = 1 AND t.is_disposed = 0
            """)
    Long countUndisposedAlarms(@Param("userId") Long userId);


    @Select("""
                SELECT SUBSTRING_INDEX(a.drone_model, ' ', 1) AS brand, COUNT(*) AS sortie_count
                FROM alarm a
                INNER JOIN device d ON a.scanID = d.id
                WHERE d.device_user_id = #{userId}
                  AND a.intrusion_start_time >= CURDATE()
                  AND a.intrusion_start_time < CURDATE() + INTERVAL 1 DAY
                GROUP BY brand
            """)
    List<Map<String, Object>> countFlightByBrand(@Param("userId") Long userId);


    @Select("""
                SELECT LPAD(HOUR(a.intrusion_start_time), 2, '0') AS hourStr, COUNT(*) AS sortieCount
                FROM alarm a
                INNER JOIN device d ON a.scanID = d.id
                WHERE d.device_user_id = #{userId}
                  AND a.intrusion_start_time >= CURDATE()
                  AND a.intrusion_start_time < CURDATE() + INTERVAL 1 DAY
                GROUP BY hourStr
                ORDER BY hourStr
            """)
    List<Map<String, Object>> countSortieByHour(@Param("userId") Long userId);


}




