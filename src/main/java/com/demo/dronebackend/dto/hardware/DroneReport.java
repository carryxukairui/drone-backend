package com.demo.dronebackend.dto.hardware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
public class DroneReport {

    // 用于标识厂商，仅在内部使用
    @JsonIgnore
    private String vendor;

    private String stationId;

    // 不符合命名规范，反序列化会失败，需加注解标识
    @JsonProperty("Drone")
    private Object Drone;

    private Integer detectType;

    private Date intrusionStartTime;

    private Double longitude;

    private Double latitude;

    private Double height;

    private Double frequency;

    private Double bandwidth;

    private Double speed;

    private Double horizontalHeadingAngle;

    private Double verticalHeadingAngle;

    private String droneModel;

    private Integer type;

    private Double lastingTime;

    private Object scanId;

    private String id;

    private String droneUUID;

    private Double backLongitude;

    private Double backLatitude;

}
