package com.demo.dronebackend.util;


import com.demo.dronebackend.config.DroneMappingConfig;
import com.demo.dronebackend.dto.hardware.DroneReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 用于映射硬件上报的JSON数据到DroneReport对象
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DroneReportMapper {

    private final DroneMappingConfig config;
    private final ObjectMapper objectMapper;


    public DroneReport mapWithVendor(JsonNode raw, String vendor) {
        Map<String, String> fieldMap = config.getMappings().get(vendor);
        DroneReport report = new DroneReport();
        Class<?> clazz = DroneReport.class;

        for (Field field : clazz.getDeclaredFields()) {
            String fieldName = field.getName();
            String jsonField = fieldName;  // 默认和字段名一致

            if (fieldMap != null && fieldMap.containsKey(fieldName)) {
                String configured = fieldMap.get(fieldName);
                if (configured != null && !configured.isEmpty()) {
                    jsonField = configured;
                }
            }

            JsonNode valueNode = raw.get(jsonField);
            if (valueNode == null || valueNode.isNull()) continue;

            try {
                field.setAccessible(true);
                Object value = objectMapper.treeToValue(valueNode, field.getType());
                field.set(report, value);
            } catch (Exception e) {
                log.warn("字段映射失败: {} <- {}，原因: {}", fieldName, jsonField, e.getMessage());
            }
        }

        report.setVendor(vendor);
        return report;
    }
}
