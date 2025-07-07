package com.demo.dronebackend.dto.admin;

import lombok.Data;

@Data
public class UserQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String name;
    private String phone;
    private String permission;
}
