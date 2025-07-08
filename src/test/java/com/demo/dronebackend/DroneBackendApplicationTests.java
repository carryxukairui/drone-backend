package com.demo.dronebackend;

import com.demo.dronebackend.util.SaltUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DroneBackendApplicationTests {


    @Test
    void contextLoads() {
        String s = SaltUtil.generateSalt();
        System.out.println(s+"，长度为:"+s.length());
    }

}
