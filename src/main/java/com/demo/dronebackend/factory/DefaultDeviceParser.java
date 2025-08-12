package com.demo.dronebackend.factory;


import cn.hutool.core.util.StrUtil;
import com.demo.dronebackend.dto.hardware.DefaultDeviceReport;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.model.DeviceConvertible;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultDeviceParser implements DeviceReportParser{
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(JsonNode jsonNode) {
        return jsonNode.has("scannerD") && jsonNode.has("station_id");
    }

    @Override
    public List<DeviceConvertible> parse(JsonNode jsonNode) throws Exception {
        String stationId = jsonNode.get("station_id").asText();
        List<DeviceConvertible> list = new ArrayList<>();

        for (JsonNode deviceNode : jsonNode.get("scannerD")) {
            DefaultDeviceReport report = objectMapper.treeToValue(deviceNode, DefaultDeviceReport.class);
            report.setStationId(stationId);

            fillDefaults(report);
            list.add(report);
        }
        return list;
    }

    private void fillDefaults(DefaultDeviceReport report){
        if(report.getLng() == null) report.setLng(120.72);
        if(report.getLat() == null) report.setLat(30.5278);
        if(report.getIp() == null) report.setIp("UNKNOWN");
        if(StrUtil.isBlank(report.getStationId())) report.setStationId("UNKNOWN");
        if (StrUtil.isBlank(report.getId())) report.setId("UNKNOWN");
    }
}
