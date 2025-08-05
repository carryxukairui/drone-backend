package com.demo.dronebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import com.demo.dronebackend.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler
    public Result<?> globalException(Exception e){
        log.error("全局异常捕获 | 错误类型: {} | 详情: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> illegalArgumentExceptionHandler(IllegalArgumentException e){
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaTokenException(SaTokenException e) {
        log.warn("业务异常 | 错误码: {} | 原因: {}", e.getCode(), e.getMessage());

        return Result.error(e.getMessage());
    }
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException e) {
        log.warn("未登录异常 | 错误码: {} | 错误信息: {}", e.getCode(), e.getMessage());
        return  Result.notLogin(e.getMessage());
    }
}
