package com.demo.dronebackend.factory;


import com.demo.dronebackend.model.DeviceConvertible;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeviceReportParserFactory {
    private final List<DeviceReportParser> parsers;
    public DeviceReportParserFactory(List<DeviceReportParser> parsers) {
        this.parsers = parsers;
    }
    public List<DeviceConvertible> parse(JsonNode jsonNode) throws Exception{
        for (DeviceReportParser parser : parsers) {
            if (parser.supports(jsonNode)) {
                return parser.parse(jsonNode);
            }
        }
        throw new IllegalArgumentException("不支持的厂商数据格式");
    }
}
