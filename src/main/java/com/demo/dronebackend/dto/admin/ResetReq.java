package com.demo.dronebackend.dto.admin;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.PASSWORD_REGEX;

@Data
public class ResetReq {
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = PASSWORD_REGEX, message = "密码格式不正确")
    private String newPassword;
}
