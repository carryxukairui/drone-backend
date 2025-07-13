package com.demo.dronebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tianditu.reverse-geocode")
public class TiandituProperties {
    private String url;
    private String key;
}
