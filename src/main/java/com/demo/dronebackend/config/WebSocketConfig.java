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

import static com.demo.dronebackend.constant.SystemConstants.SA_TOKEN;


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
                        URI uri = request.getURI();
                        String query = uri.getQuery(); // 例：token=eyJhbGciOiJIUzI1NiIsInR
                        if (query != null && query.startsWith(SA_TOKEN)) {
                            String token = query.substring(SA_TOKEN.length());
                            // 登录校验
                            StpUtil.checkLogin();
                            String userId = StpUtil.getLoginIdByToken(token).toString();
                            if (userId != null) {
                                String topicKey = SystemConstants.DEVICES_WEBSOCKET_TOPIC + ":" + userId;
                                // 4. 把 userId 放到 WebSocketSession 属性里
                                attributes.put(SystemConstants.DEVICES_WEBSOCKET_TOPIC, topicKey);
                                return true;
                            }
                        }
                        // 无效 token，拦截连接
                        return false;
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
                        URI uri = request.getURI();
                        String query = uri.getQuery(); // 例：token=eyJhbGciOiJIUzI1NiIsInR
                        if (query != null && query.startsWith(SA_TOKEN)) {
                            String token = query.substring(SA_TOKEN.length());
                            // 登录校验
                            StpUtil.checkLogin();
                            String userId = StpUtil.getLoginIdByToken(token).toString();
                            if (userId != null) {
                                String topicKey = SystemConstants.ALARM_WEBSOCKET_TOPIC + ":" + userId;
                                // 4. 把 userId 放到 WebSocketSession 属性里
                                attributes.put(SystemConstants.ALARM_WEBSOCKET_TOPIC, topicKey);
                                return true;
                            }
                        }
                        // 无效 token，拦截连接
                        return false;
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