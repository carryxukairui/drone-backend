package com.demo.dronebackend;

import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.service.TiandituService;
import com.demo.dronebackend.util.SaltUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DroneBackendApplicationTests {

    @Resource
    private TiandituService tiandituService;

    @Test
    void contextLoads() {
        String location = tiandituService.reverseGeocode(120.386, 36.068);
        System.out.println(location);
    }

}
