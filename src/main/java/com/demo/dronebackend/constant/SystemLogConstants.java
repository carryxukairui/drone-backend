package com.demo.dronebackend.constant;

public class SystemLogConstants {
    public static final String LOG_TYPE_SYSTEM = "system";
    //开启无人值守模式
    public static final String OP_TYPE_UNATTENDED_START = "UNATTENDED_START";
    //关闭无人值守模式
    public static final String OP_TYPE_UNATTENDED_STOP = "UNATTENDED_STOP";
    //无人值守模式事件
    public static final String OP_TYPE_UNATTENDED_EVENT = "UNATTENDED_EVENT";
    //无人值守非黑无人机
    public static  final String OP_TYPE_UNATTENDED_NO_DRONE = "UNATTENDED_NO_DRONE";
    // 在核心区/反制区内
    public static  final String OP_TYPE_UNATTENDED_IN_AREA = "UNATTENDED_IN_AREA";
    // 离开核心区/反制区内
    public static  final String OP_TYPE_UNATTENDED_OUT_AREA = "UNATTENDED_OUT_AREA";
    // 无人值守MQTT消息发送成功
    public static final String OP_TYPE_UNATTENDED_MQTT_SUCCESS = "UNATTENDED_MQTT_SUCCESS";
    // 无人值守MQTT消息发送失败
    public static final String OP_TYPE_UNATTENDED_MQTT_FAIL = "UNATTENDED_MQTT_FAIL";
    public static final String DEVICE_PARAM_EVENT = "PARAMSETTINGS_EVENT";
}
