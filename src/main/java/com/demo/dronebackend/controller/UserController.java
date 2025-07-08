package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    /*
    密码登录
     */
    @PostMapping("/login-pwd")
    public Result loginByPassword(@Valid @RequestBody LoginByPswReq req) {
        return userService.loginByPassword(req);
    }

    /*
    修改密码
     */
    @PostMapping("/change-pwd")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordReq req) {
        return userService.updatePassword(req);
    }
    /*
    忘记密码
     */
    @PostMapping("/reset-pwd")
    public Result<?> resetPassword(@Valid @RequestBody ResetReq req) {
        return userService.resetPassword(req);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public Result<?> sendCode(@Valid @RequestBody SendCodeReq req) {
        return userService.sendCode(req);
    }

    /**
     * 验证码登录
     */
    @PostMapping("/login-code")
    public Result<?> loginByCode(@Valid @RequestBody LoginByCodeReq req) {
        return userService.loginByCode(req);
    }

}
