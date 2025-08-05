package com.demo.dronebackend.dto.hardware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Null;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
public class DroneReport {

    // 用于标识厂商，仅在内部使用
//    @JsonIgnore
//    private String vendor;

    private String station_id;

    // 不符合命名规范，反序列化会失败，需加注解标识
    @JsonProperty("Drone")
    private Object Drone;

    private Integer detect_type;

    private Date intrusion_start_time;

    private Double longitude;

    private Double latitude;

    private Double height;

    private Double frequency;

    private Double bandwidth;

    private Double speed;

    private Double horizontal_heading_angle;

    private Double vertical_heading_angle;

    private String model;

    private Integer type;

    private Double lastingTime;

    private Object scanId;

    private String id;

    private String drone_uuid;

    private Double backLongitude;

    private Double backLatitude;

}
