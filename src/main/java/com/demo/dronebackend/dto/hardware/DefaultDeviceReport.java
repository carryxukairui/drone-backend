package com.demo.dronebackend.dto.hardware;

import cn.hutool.core.date.DateTime;
import com.demo.dronebackend.mapper.DeviceMapper;
import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.pojo.Device;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;


@Data
@NoArgsConstructor // Jackson 需要无参构造
public class DefaultDeviceReport implements DeviceConvertible {


    @JsonProperty("station_id")
    private String stationId;

    private String id;
    @JsonProperty("link_state")
    private Integer linkState;
    @JsonProperty("data_rate")
    private Double data_rate;
    @JsonProperty("foundTarget")
    private Integer foundTarget;
    private Double lng;
    private Double lat;
    private String ip;

    @Override
    public Device toDevice() {
        Device dev = new Device();
        dev.setId(this.id);
        dev.setStationId(this.stationId);
        dev.setLinkStatus(this.linkState);
        dev.setLongitude(this.lng != null ? this.lng : 120.72);
        dev.setLatitude(this.lat != null ? this.lat : 30.527);
        dev.setIp(this.ip != null ? this.ip : "UNKNOWN");

        System.out.println("UNKNOWN device:"+dev);
        return dev;
    }
}

