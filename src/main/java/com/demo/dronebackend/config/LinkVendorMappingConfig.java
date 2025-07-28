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
public class LinkVendorMappingConfig implements InitializingBean {

    // key = vendor, value = 字段映射表
    private Map<String, Map<String, String>> droneMappings = new HashMap<>();

    private Map<String, Map<String, String>>  deviceMappings = new HashMap<>();
    @Override
    public void afterPropertiesSet() {
        try (InputStream input = new ClassPathResource("drone-report-mapping.yaml").getInputStream()) {
            Yaml yaml = new Yaml();
            this.droneMappings = yaml.load(input);
            log.info("已加载无人机 {} 个厂商字段映射配置", droneMappings.size());
        } catch (Exception e) {
            log.error("加载 drone-report-mapping.yaml 失败", e);
        }
        try (InputStream input = new ClassPathResource("device-report-mapping.yaml").getInputStream()) {
            Yaml yaml = new Yaml();
            this.deviceMappings = yaml.load(input);
            log.info("已加载设备 {} 个厂商字段映射配置", deviceMappings.size());
        } catch (Exception e) {
            log.error("加载 device-report-mapping.yaml 失败", e);
        }
    }
}
