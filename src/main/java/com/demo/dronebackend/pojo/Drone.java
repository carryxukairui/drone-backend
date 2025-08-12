package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @TableName drone
 */
@TableName(value ="drone")
@Data
public class Drone {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String droneBrand;

    private String droneModel;

    @NotBlank
    private String droneSn;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @NotBlank
    private String type;
    @NotBlank
    private Long userId;
}