package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

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
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private String type;

    private Long userId;
}