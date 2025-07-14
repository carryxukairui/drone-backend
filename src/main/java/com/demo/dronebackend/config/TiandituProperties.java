package com.demo.dronebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tianditu")
public class TiandituProperties {

    private ApiConfig reverseGeocode;
    private ApiConfig geocode;

    @Data
    public static class ApiConfig {
        private String url;
        private String key;
    }
}
