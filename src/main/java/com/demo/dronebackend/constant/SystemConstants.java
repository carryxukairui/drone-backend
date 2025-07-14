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
    public static final String SA_TOKEN = "satoken";

    /**
     * 合并指定时间范围内同一无人机触发的所有轨迹信息，可根据业务调整
     * 默认为24小时
     */
    public static final long TRAJECTORY_TIME = 24L * 60 * 60 * 1000;

    public static final String ALARM_WEBSOCKET_TOPIC = "alarm";
    public static final String DEVICES_WEBSOCKET_TOPIC = "devices";
}
