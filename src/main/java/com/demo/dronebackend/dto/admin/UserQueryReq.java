package com.demo.dronebackend.dto.admin;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.PHONE_REGEX;

@Data
public class UserQueryReq {
    private Integer page = 1;

    private Integer size = 10;

    private String name;

    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    private String permission;
}
