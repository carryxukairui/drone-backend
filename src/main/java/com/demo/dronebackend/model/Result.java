package com.demo.dronebackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // 新增带状态码的静态方法
    public static <T> Result<T> custom(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }


    // 保持原有方法...
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "", data);
    }
    public static <T> Result<T> error(String msg) {
        return new Result<T>(1, msg, null);
    }
    //查询 成功响应
    public static <T> Result<T> success(String msg ,T data) {
        return new Result<T>(200, msg, data);
    }

    public static Object notLogin(String 未登录) {
        return new Result<>(401, 未登录, null);
    }
}