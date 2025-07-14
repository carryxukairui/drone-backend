package com.demo.dronebackend.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import static com.demo.dronebackend.constant.SystemConstants.PHONE_REGEX;

@Data
public class UserQueryReq {

    /** 页码，默认 1 */
    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    /** 每页条数，默认 10 */
    @Min(value = 1, message = "size 必须 ≥ 1")
    private Integer size = 10;

    private String name;

//    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    private String permission;
}
