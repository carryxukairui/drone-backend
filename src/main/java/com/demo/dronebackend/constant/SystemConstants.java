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
     *
     * 请求体的satoken
     */
    public static final String SA_TOKEN = "satoken";


}
