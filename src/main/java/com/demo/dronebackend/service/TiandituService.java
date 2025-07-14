package com.demo.dronebackend.service;

import com.demo.dronebackend.config.TiandituProperties;
import com.demo.dronebackend.model.GeocodeLocation;
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
                .fromHttpUrl(props.getReverseGeocode().getUrl())
                .queryParam("postStr", postStr)
                .queryParam("type", "geocode")
                .queryParam("tk", props.getReverseGeocode().getKey())
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


    /**
     * 调用天地图地理编码（地址 → 经纬度）
     */
    public GeocodeLocation geocode(String address) {
        // 1. 构造 ds 参数，注意要用双引号
        String dsJson = String.format("{\"keyWord\":\"%s\"}", address);

        // 2. 构造完整 URL 并编码
        String url = UriComponentsBuilder
                .fromHttpUrl(props.getGeocode().getUrl())
                .queryParam("ds",     URLEncoder.encode(dsJson, StandardCharsets.UTF_8))
                .queryParam("tk",     props.getGeocode().getKey())
                .toUriString();

        // 3. 发起请求
        String resp = restTemplate.getForObject(url, String.class);

        // 4. 解析 JSON
        try {
            JsonNode root = objectMapper.readTree(resp);
            String status = root.path("status").asText();
            if (!"0".equals(status)) {
                throw new RuntimeException("地理编码 API 返回异常，status=" + status);
            }
            JsonNode loc = root.path("location");
            // 手动取值并转换类型
            double lon   = loc.path("lon").asDouble();
            double lat   = loc.path("lat").asDouble();
            int    score = loc.path("score").asInt();
            String level = loc.path("level").asText(null);
            String kw    = loc.path("keyWord").asText(null);
            return new GeocodeLocation(lon, lat, score, level, kw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析地理编码响应失败", e);
        }
    }
}
