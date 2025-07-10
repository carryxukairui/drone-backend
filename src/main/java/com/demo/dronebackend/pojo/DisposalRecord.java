package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @TableName disposal_record
 */
@TableName(value ="disposal_record")
@Data
public class DisposalRecord {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    private Double duration;

    private String deviceId;

    private Integer g09Onoff;

    private Integer g16Onoff;

    private Integer g24Onoff;

    private Integer g58Onoff;
}