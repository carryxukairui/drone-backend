package com.demo.dronebackend.dto.hardware;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class DroneReport {

    private String stationId;

    // 不符合命名规范，反序列化可能失败，需加注解标识
    @JsonProperty("Drone") // TODO: 建议改名
    private List<Map<Object,Object>> Drone;

    private Integer detectType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    private List<String> scanId;

    private String id;

    private String droneUUID;

    private Double backLongitude;

    private Double backLatitude;

}
