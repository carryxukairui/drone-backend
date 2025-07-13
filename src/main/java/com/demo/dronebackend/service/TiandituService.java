package com.demo.dronebackend.service;

import com.demo.dronebackend.config.TiandituProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TiandituService {

    private final RestTemplate restTemplate;
    private final TiandituProperties props;
    private final ObjectMapper objectMapper;

    /**
     * 根据经纬度调用天地图逆地理解析，返回结构化地址信息
     */
    public String reverseGeocode(double lon, double lat) {

        String postStr = String.format("{'lon':%f,'lat':%f,'ver':1}", lon, lat);
        System.out.println(postStr);

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getUrl())
                .queryParam("postStr", postStr)
                .queryParam("type", "geocode")
                .queryParam("tk", props.getKey())
                .toUriString();

        // 2. 发起 GET 请求
        String resp = restTemplate.getForObject(url, String.class);


        try {
            JsonNode root = objectMapper.readTree(resp);
            JsonNode result = root.path("result");
            // 你可以根据接口文档，定位到城市、省份、街道等字段
            String formatted = result.path("formatted_address").asText();
            return formatted;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析天地图响应失败", e);
        }
    }
}
