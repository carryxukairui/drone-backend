package com.demo.dronebackend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @TableName disposal_record
 */
@TableName(value ="disposal_record")
@Data
public class DisposalRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    private Double duration;

    private String deviceId;

    private Integer g09Onoff;

    private Integer g16Onoff;

    private Integer g24Onoff;

    private Integer g58Onoff;
}