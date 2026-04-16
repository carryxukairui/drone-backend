package com.demo.dronebackend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基类
 * 所有测试类应继承此类以获得统一的测试配置
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseTest {
    // 测试基类，提供统一的SpringBoot测试环境配置
}
