package com.demo.dronebackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 添加人员请求体
 */
@Data
public class AddUserRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    private String sex;

    @NotBlank(message = "单位名称不能为空")
    private String organization;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{10,11}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
