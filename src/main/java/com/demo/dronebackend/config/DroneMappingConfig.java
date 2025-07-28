package com.demo.dronebackend.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置类，用于加载 drone-report-mapping.yaml 文件
 */
@Configuration
@Slf4j
@Getter
public class DroneMappingConfig implements InitializingBean {

    // key = vendor, value = 字段映射表
    private Map<String, Map<String, String>> mappings = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        try (InputStream input = new ClassPathResource("drone-report-mapping.yaml").getInputStream()) {
            Yaml yaml = new Yaml();
            this.mappings = yaml.load(input);
            log.info("已加载 {} 个厂商字段映射配置", mappings.size());
        } catch (Exception e) {
            log.error("加载 drone-report-mapping.yaml 失败", e);
        }
    }
}
