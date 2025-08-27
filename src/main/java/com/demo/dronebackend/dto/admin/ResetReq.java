package com.demo.dronebackend.dto.admin;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.CODE_REGEX;
import static com.demo.dronebackend.constant.SystemConstants.PASSWORD_REGEX;

@Data
public class ResetReq {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = CODE_REGEX, message = "验证码格式不正确")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = PASSWORD_REGEX, message = "密码格式不正确")
    private String newPassword;
}
