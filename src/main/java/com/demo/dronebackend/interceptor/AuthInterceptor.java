package com.demo.dronebackend.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.util.CurrentUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {


    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!StpUtil.isLogin()) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.notLogin("未登录")));
            return false;
        }
        // 1) 从 SaToken 拿当前登录的 userId
        Long userId = StpUtil.getLoginIdAsLong();
        // 2) 从数据库查 User 实体
        User user = userMapper.selectById(userId);
        // 3) 放入 ThreadLocal
        CurrentUserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 一定要清理
        CurrentUserContext.clear();
    }
}
