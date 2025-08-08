package com.demo.dronebackend.factory;

import com.demo.dronebackend.model.AlarmConvertible;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 无人机侦测数据上报解析器接口
 */
public interface DroneReportParser {
    // 判断是否支持解析
    boolean supports(JsonNode jsonNode);

    // 解析数据
    List<AlarmConvertible> parse(JsonNode jsonNode) throws Exception;
}