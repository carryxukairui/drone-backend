package com.demo.dronebackend.dto.admin;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeReq {
    @NotBlank(message = "手机号不能为空")
    private String phone;
    @NotBlank(message = "签名验证不能为空")
    private String sign;
}
