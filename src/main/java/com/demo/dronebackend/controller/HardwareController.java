package com.demo.dronebackend.controller;

import com.demo.dronebackend.dto.hardware.DeviceReport;
import com.demo.dronebackend.factory.DroneReportParserFactory;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.service.AlarmService;
import com.demo.dronebackend.service.DeviceService;
import com.demo.dronebackend.util.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HardwareController {
    private final AlarmService alarmService;
    private final DeviceService deviceService;
    private final DroneReportParserFactory droneReportParserFactory;

    /**
     * 响应硬件发送请求
     * @param jsonNode 无人机原始侦测上报数据
     */
    @PostMapping("sys/portable/drone/report")
    public Result<?> reportDrone(@RequestBody JsonNode jsonNode) {
        try {
            List<AlarmConvertible> reports = droneReportParserFactory.parse(jsonNode);
            reports.forEach(alarmService::handleDroneReport);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("解析失败: " + e.getMessage());
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
     * websocket获取硬件数据
     *
     * @param
     * @return
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
