package com.demo.dronebackend.factory;

import com.demo.dronebackend.dto.hardware.DefaultDroneReport;
import com.demo.dronebackend.model.AlarmConvertible;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultDroneParser implements DroneReportParser {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(JsonNode jsonNode) {
        // 根据 jsonNode 字段判断是否支持此厂商硬件上报数据
        return jsonNode.has("drone") && jsonNode.has("station_id");
    }

    @Override
    public List<AlarmConvertible> parse(JsonNode jsonNode) throws Exception {
        String stationId = jsonNode.get("station_id").asText();
        List<AlarmConvertible> list = new ArrayList<>();

        for (JsonNode droneNode : jsonNode.get("drone")) {
            DefaultDroneReport report = objectMapper.treeToValue(droneNode, DefaultDroneReport.class);
            report.setStation_id(stationId);

            if (report.getScanID() != null && !report.getScanID().isEmpty()) {
                report.setId(report.getScanID().get(0).getId());
            }
            list.add(report);
        }
        return list;
    }
}