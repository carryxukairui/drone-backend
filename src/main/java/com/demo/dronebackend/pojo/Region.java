package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName region
 */
@TableName(value ="region")
@Data
public class Region {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Date time;

    private Integer type;

    private Long userId;

    private Double centerLon;

    private Double centerLat;

    private Double radius;
}