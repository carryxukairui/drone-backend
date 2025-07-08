package com.demo.dronebackend.dto.admin;


import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.PHONE_REGEX;

@Data
public class UpdateUserReq {

    private String name;

    private String sex;

    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    private String organization;

    private String permission;

}
