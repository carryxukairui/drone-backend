package com.demo.dronebackend.mapper;

import com.demo.dronebackend.pojo.Region;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 28611
* @description 针对表【region(用户自定义区域表（预警区/反制区/核心区）)】的数据库操作Mapper
* @createDate 2025-07-14 13:54:34
* @Entity com.demo.dronebackend.pojo.Region
*/
public interface RegionMapper extends BaseMapper<Region> {

    /**
     * 查询给定用户、给定类型列表的所有区域
     */
    @Select({
            "<script>",
            "SELECT *",
            "  FROM region",
            " WHERE user_id = #{userId}",
            "   AND type IN ",
            "     <foreach collection='types' item='t' open='(' separator=',' close=')'>",
            "       #{t}",
            "     </foreach>",
            "</script>"
    })
    List<Region> selectByUserAndTypes(
            @Param("userId") long userId,
            @Param("types")  List<Integer> types
    );
}




