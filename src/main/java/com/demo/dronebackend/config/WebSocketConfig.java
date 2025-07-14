package com.demo.dronebackend.config;

import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.constant.SystemConstants;
import com.demo.dronebackend.ws.AlarmWebSocketHandler;
import com.demo.dronebackend.ws.MyWebSocketHandler;
import com.demo.dronebackend.ws.WebSocketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketService webSocketService;

    public WebSocketConfig(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/ws/device")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {
                        // 1. 强转到 ServletRequest，才能取到 Sa‑Token 存在的 header/cookie/param
                        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                        HttpServletRequest httpReq = servletRequest.getServletRequest();

                        // 2. 从请求里拿到 Sa‑Token（默认名称为 "satoken"），可在 Header、Cookie、参数里
                        String token = httpReq.getHeader(SystemConstants.SA_TOKEN);
                        if (token == null) {
                            token = httpReq.getParameter(SystemConstants.SA_TOKEN);
                        }

                        StpUtil.checkLogin();                // 校验：没登录会抛异常
                        String loginId = StpUtil.getLoginIdByToken(token).toString();

                        // 4. 把 userId 放到 WebSocketSession 属性里
                        attributes.put("userId", loginId);
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                    }
                }).setAllowedOrigins("*");

        registry.addHandler(alarmHandler(), "/ws/alarms")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {
                        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                        HttpServletRequest httpReq = servletRequest.getServletRequest();

                        // 从请求里拿到token
                        String token = httpReq.getHeader(SystemConstants.SA_TOKEN);
                        if (token == null) {
                            token = httpReq.getParameter(SystemConstants.SA_TOKEN);
                        }
                        // 登录校验
                        StpUtil.checkLogin();
                        String userId = StpUtil.getLoginIdByToken(token).toString();

                        // 把 userId 放到 WebSocketSession
                        attributes.put("userId", userId);
                        return true;
                    }
                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                    }
                }).setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler myHandler() {
        return new MyWebSocketHandler(webSocketService);
    }

    @Bean
    public WebSocketHandler alarmHandler() {
        return new AlarmWebSocketHandler(webSocketService);
    }
}