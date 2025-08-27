package com.demo.dronebackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.PHONE_REGEX;

/**
 * 添加人员请求体
 */
@Data
public class AddUserReq {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "性别不能为空")
    private String sex;

    @NotBlank(message = "单位名称不能为空")
    private String organization;

    @NotBlank(message = "用户权限不能为空")
    private String permission;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;
}
