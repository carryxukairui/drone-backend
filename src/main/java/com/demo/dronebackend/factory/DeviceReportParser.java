package com.demo.dronebackend.factory;

import cn.hutool.core.util.StrUtil;
import com.demo.dronebackend.dto.hardware.DefaultDeviceReport;
import com.demo.dronebackend.model.AlarmConvertible;
import com.demo.dronebackend.model.DeviceConvertible;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/*
 * @Description: 设备数据解析接口
 *
 */
public interface DeviceReportParser {

    //判断是否支持解析
    boolean supports(JsonNode jsonNode);

    //解析数据
    List<DeviceConvertible> parse(JsonNode jsonNode) throws Exception;


}
