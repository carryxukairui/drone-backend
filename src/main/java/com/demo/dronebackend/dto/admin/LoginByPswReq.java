package com.demo.dronebackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.*;

@Data
public class LoginByPswReq {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = PASSWORD_REGEX, message = "密码格式不正确")
    private String password;
}
