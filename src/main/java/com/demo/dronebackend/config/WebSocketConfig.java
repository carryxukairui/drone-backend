package com.demo.dronebackend.config;

import com.demo.dronebackend.ws.MyWebSocketHandler;
import com.demo.dronebackend.ws.WebSocketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketService webSocketService;

    public WebSocketConfig(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler myHandler() {
        return new MyWebSocketHandler(webSocketService);
    }
}