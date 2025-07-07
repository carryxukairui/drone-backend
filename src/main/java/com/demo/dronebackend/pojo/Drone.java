package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName drone
 */
@TableName(value ="drone")
@Data
public class Drone {
    private Long id;

    private String droneBrand;

    private String droneModel;

    private String droneSn;

    private Date updateTime;

    private String type;

    private Long userId;
}