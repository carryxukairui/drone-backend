package com.demo.dronebackend.factory;

import cn.hutool.core.util.StrUtil;
import com.demo.dronebackend.dto.hardware.DefaultDroneReport;
import com.demo.dronebackend.model.AlarmConvertible;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
            // 如果为空或关键字段缺失则跳过该条数据
            if (report == null || isInvalidData(report)) continue;
            report.setStation_id(stationId);
            if (report.getScanID() != null && !report.getScanID().isEmpty()) {
                report.setId(report.getScanID().get(0).getId());
            }
            fillDefaults(report);
            list.add(report);
        }
        return list;
    }

    private boolean isInvalidData(DefaultDroneReport report) {
        return report.getDrone_uuid().equals("UNKNOWN_DRONE_UUID") && report.getModel().equals("Unknown Drone Model");
    }

    // 填充默认值
    private void fillDefaults(DefaultDroneReport report) {
        if (report.getLongitude() == null) report.setLongitude(0.0);
        if (report.getLatitude() == null) report.setLatitude(0.0);
        if (report.getHeight() == null) report.setHeight(0.0);
        if (report.getLasting_time() == null) report.setLasting_time(0.0);
        if (report.getHorizontal_heading_angle() == null) report.setHorizontal_heading_angle(0.0);
        if (report.getVertical_heading_angle() == null) report.setVertical_heading_angle(0.0);
        if (report.getScanID() == null) report.setScanID(new ArrayList<>());
        if (report.getOp_Lon() == null) report.setOp_Lon(400.0);
        if (report.getOp_Lat() == null) report.setOp_Lat(400.0);
        if (StrUtil.isBlank(report.getStation_id())) report.setStation_id("UNKNOWN");
        if (StrUtil.isBlank(report.getId())) report.setId("UNKNOWN");
        if (StrUtil.isBlank(report.getDrone_uuid())) report.setDrone_uuid("UNKNOWN");
        if (StrUtil.isBlank(report.getIntrusion_start_time())) {
            report.setIntrusion_start_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
    }
}