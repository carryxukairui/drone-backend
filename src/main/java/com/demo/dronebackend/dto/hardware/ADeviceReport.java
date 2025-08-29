package com.demo.dronebackend.dto.hardware;

import com.demo.dronebackend.model.DeviceConvertible;
import com.demo.dronebackend.pojo.Device;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ADeviceReport implements DeviceConvertible {


    @JsonProperty("station_id")
    private String stationId;

    private String id;
    @JsonProperty("link_status")
    private Integer linkState;
    @JsonProperty("data_rate")
    private Double data_rate;
    @JsonProperty("foundTarget")
    private Integer foundTarget;
    private Double lng;
    private Double lat;
    private String ip;
    private Double tempeature;
    @Override
    public Device toDevice() {
        Device dev = new Device();
        dev.setId(this.id);
        dev.setStationId(this.stationId);
        dev.setLinkStatus(this.linkState);
        dev.setLongitude(this.lng != null ? this.lng : 120.72);
        dev.setLatitude(this.lat != null ? this.lat : 30.527);
        dev.setIp(this.ip != null ? this.ip : "UNKNOWN");
        dev.setTemperature(this.tempeature != null ? this.tempeature : 0.0);
        return dev;
    }
}
