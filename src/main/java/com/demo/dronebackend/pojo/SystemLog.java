package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName system_log
 */
@TableName(value ="system_log")
@Data
public class SystemLog {
    private Long id;

    private Long userId;

    private String username;

    private String operationType;

    private String description;

    private Date createdTime;
}