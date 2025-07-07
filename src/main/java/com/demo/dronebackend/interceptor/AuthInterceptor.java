package com.demo.dronebackend.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!StpUtil.isLogin()) {
            ObjectMapper objectMapper = new ObjectMapper();
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.notLogin("未登录")));
            return false;
        }
        return true;
    }
}
