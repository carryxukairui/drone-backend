package com.demo.dronebackend.controller;

import com.demo.dronebackend.dto.hardware.DeviceReport;
import com.demo.dronebackend.dto.hardware.DroneReport;
import com.demo.dronebackend.model.ReportVendor;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HardwareController {
    private final AlarmService alarmService;
    private final DeviceService deviceService;
    private final ReportVendor reportVendor;
    @PostMapping("sys/portable/drone/report")
    public Result<?> reportDrone(HttpServletRequest request) {
        try {
            // 读取请求体内容
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = request.getReader();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String requestBody = stringBuilder.toString();

            // 检查请求体是否为空
            if (requestBody.isEmpty()) {
                return Result.error("请求体不能为空");
            }

            // 手动解析JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            String stationId = jsonNode.get("station_id") != null ? jsonNode.get("station_id").asText() : null;

            // 处理 drone 数组
            if (jsonNode.has("drone") && jsonNode.get("drone").isArray()) {
                JsonNode droneArray = jsonNode.get("drone");

                // 循环处理每个 drone 对象
                for (JsonNode droneNode : droneArray) {
                    // 创建 DroneReport 对象并填充数据
                    DroneReport report = new DroneReport();
                    report.setStation_id(stationId);

                    // 设置各个字段
                    report.setDetect_type(getIntValue(droneNode, "detect_type"));
                    report.setDrone_uuid(getStringValue(droneNode, "drone_uuid"));
                    report.setModel(getStringValue(droneNode, "model"));
                    report.setType(getIntValue(droneNode, "type"));
                    report.setLatitude(getDoubleValue(droneNode, "latitude"));
                    report.setLongitude(getDoubleValue(droneNode, "longitude"));
                    report.setHeight(getDoubleValue(droneNode, "height"));
                    report.setFrequency(getDoubleValue(droneNode, "frequency"));
                    report.setBandwidth(getDoubleValue(droneNode, "bandwidth"));
                    report.setSpeed(getDoubleValue(droneNode, "speed"));
                    report.setHorizontal_heading_angle(getDoubleValue(droneNode, "horizontal_heading_angle"));
                    report.setVertical_heading_angle(getDoubleValue(droneNode, "vertical_heading_angle"));
                    report.setLastingTime(getDoubleValue(droneNode, "lasting_time"));

                    // 特殊处理 intrusion_start_time (空字符串情况)
                    if (droneNode.has("intrusion_start_time")) {
                        String timeStr = droneNode.get("intrusion_start_time").asText();
                        if (timeStr != null && !timeStr.isEmpty() && !"null".equals(timeStr)) {
                            // 如果需要解析具体日期，可以在这里添加日期解析逻辑
                            // report.setIntrusion_start_time(parsedDate);
                        }
                        // 空字符串则保持为 null
                    }

                    // 处理 scanID
                    if (droneNode.has("scanID")) {
                        JsonNode node = droneNode.get("scanID");
                        for(JsonNode n: node){
                            String id = getStringValue(n, "id");
                            report.setId(id);
                        }
                        report.setScanId(droneNode.get("scanID"));

                    }

                    // 调用服务处理每个 drone 报告
                    alarmService.handleDroneReport(report);
                }
            }
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("请求体解析失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ?
                node.get(fieldName).asText() : null;
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ?
                node.get(fieldName).asInt() : null;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ?
                node.get(fieldName).asDouble() : null;
    }

    /**
     * 响应硬件发送请求
     *
     * @param
     */
//    @PostMapping("sys/portable/status/report")
//    public Map<String, Object> reportStatus(@RequestBody JsonNode raw) {
//        DeviceReport report = reportVendor.DeviceWithVendor(raw, "default");
//
//        return deviceService.websocketDevice(report);
//    }

    @PostMapping("admin/devices/sub")
    public Result<?> reportStatus(HttpServletRequest request) {
        try {
            // 读取请求体内容
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = request.getReader();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            String requestBody = stringBuilder.toString();

            // 检查请求体是否为空
            if (requestBody.isEmpty()) {
                return Result.error("请求体不能为空");
            }

            // 手动解析JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            String stationId = jsonNode.get("station_id") != null ? jsonNode.get("station_id").asText() : null;

            // 处理 scannerD 数组
            if (jsonNode.has("scannerD") && jsonNode.get("scannerD").isArray()) {
                JsonNode scannerArray = jsonNode.get("scannerD");

                // 循环处理每个 scanner 对象
                for (JsonNode scannerNode : scannerArray) {
                    // 创建 DeviceReport 对象并填充数据
                    DeviceReport report = new DeviceReport();
                    report.setStationId(stationId);

                    // 设置各个字段
                    report.setId(getStringValue(scannerNode, "id"));
                    report.setIp(getStringValue(scannerNode, "ip"));
                    report.setLinkState(getIntValue(scannerNode, "link_state"));
                    if (getStringValue(scannerNode, "lat").isEmpty()){
                        report.setLat(30.735);
                    }else {
                        report.setLat(getDoubleValue(scannerNode, "lat"));
                    }
                    if (getStringValue(scannerNode, "lng").isEmpty()){
                        report.setLng(120.826);
                    }else {
                        report.setLng(getDoubleValue(scannerNode, "lng"));
                    }



                    // 调用服务处理每个设备报告
                    Map<String, Object> result = deviceService.websocketDevice(report);

                    // 检查处理结果，如果需要的话
                    if (result != null && !result.isEmpty()) {
                        // 可以根据返回结果做进一步处理
                    }
                }
            }

            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("请求体解析失败: " + e.getMessage());
        }
    }




}
