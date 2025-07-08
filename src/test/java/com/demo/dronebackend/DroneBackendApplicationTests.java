package com.demo.dronebackend;

import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.util.SaltUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DroneBackendApplicationTests {


    @Test
    void contextLoads() {
        Object obj=PermissionType.admin;
        if ( obj instanceof String) {
            System.out.println("是字符串");
        }else {
            System.out.println("NOOOOOO");
        }
    }

}
