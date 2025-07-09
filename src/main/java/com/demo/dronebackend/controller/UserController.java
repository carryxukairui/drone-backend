package com.demo.dronebackend.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 发送验证码
     * @param req 发送验证码请求体
     */
    @PostMapping("/send-code")
    public Result<?> sendCode(@Valid @RequestBody SendCodeReq req) {
        return userService.sendCode(req);
    }

    /**
     * 密码登录
     * @param req 密码登录请求体
     */
    @PostMapping("/login-pwd")
    public Result loginByPassword(@Valid @RequestBody LoginByPswReq req) {
        return userService.loginByPassword(req);
    }

    /**
     * 验证码登录
     * @param req 验证码登录请求体
     */
    @PostMapping("/login-code")
    public Result<?> loginByCode(@Valid @RequestBody LoginByCodeReq req) {
        return userService.loginByCode(req);
    }


    /**
     * 修改密码
     * @param req 修改密码请求体
     */
    @PostMapping("/change-pwd")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordReq req) {
        return userService.updatePassword(req);
    }

    /**
     * 忘记密码
     * @param req 重置密码请求体
     */
    @PostMapping("/reset-pwd")
    public Result<?> resetPassword(@Valid @RequestBody ResetReq req) {
        return userService.resetPassword(req);
    }

    /**
     * 用户登出
     */
    @GetMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success("登出成功");
    }

}
