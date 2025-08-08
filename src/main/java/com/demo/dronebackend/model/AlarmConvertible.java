package com.demo.dronebackend.model;

import com.demo.dronebackend.pojo.Alarm;

/**
 * 告警转换接口
 * 对应硬件实体类实现该方法后可自定义字段映射
 */
public interface AlarmConvertible {
    Alarm toAlarm();
}