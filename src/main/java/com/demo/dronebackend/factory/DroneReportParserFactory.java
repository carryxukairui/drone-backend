package com.demo.dronebackend.factory;

import com.demo.dronebackend.model.AlarmConvertible;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工厂类，根据JsonNode选择出合适的解析器
 */
@Component
public class DroneReportParserFactory {

    private final List<DroneReportParser> parsers;

    // Spring 会自动注入所有实现 DroneReportParser 的 Bean
    public DroneReportParserFactory(List<DroneReportParser> parsers) {
        this.parsers = parsers;
    }

    public List<AlarmConvertible> parse(JsonNode jsonNode) throws Exception {
        for (DroneReportParser parser : parsers) {
            if (parser.supports(jsonNode)) {
                return parser.parse(jsonNode);
            }
        }
        throw new IllegalArgumentException("不支持的厂商数据格式");
    }
}