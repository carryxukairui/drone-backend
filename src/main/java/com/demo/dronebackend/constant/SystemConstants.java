package com.demo.dronebackend.constant;

/**
 * 项目中用到的常量类
 */
public class SystemConstants {
    /**
     * 手机号正则
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 密码正则。6~20位的字母、数字、下划线
     */
    public static final String PASSWORD_REGEX = "^\\w{6,20}$";

    /**
     * 验证码正则, 6位数字
     */
    public static final String CODE_REGEX = "^\\d{6}$";

    /**
     * 默认初始密码，可根据业务调整
     */
    public static final String INITIAL_PASSWORD = "zhouhedikong123456";

    /**
     * 请求体的satoken
     */
    public static final String SA_TOKEN = "satoken=";

    /**
     * 查询告警记录开始时间
     * 合并无人机轨迹时间范围
     * 默认为12小时前，单位毫秒
     */
    public static final long DEFAULT_ALARM_TIME_RANGE = 12L * 60 * 60 * 1000;

    public static final String ALARM_WEBSOCKET_TOPIC = "alarm";
    public static final String DEVICES_WEBSOCKET_TOPIC = "devices";
    public static final String UNATTENDED_WEBSOCKET_TOPIC = "unattended";

    public static final String TOPIC = "device/command/startJam";
}
