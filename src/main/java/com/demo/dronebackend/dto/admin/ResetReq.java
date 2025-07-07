package com.demo.dronebackend.dto.admin;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetReq {
    @NotBlank(message = "密码不能为空")
    private String newPassword;
}
