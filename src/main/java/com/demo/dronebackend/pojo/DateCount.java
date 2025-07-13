package com.demo.dronebackend.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// 1. 修改 DateCount 为 POJO 类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateCount {
    private Date statDate;  // 属性名与查询中的别名匹配
    private Long cnt;
}