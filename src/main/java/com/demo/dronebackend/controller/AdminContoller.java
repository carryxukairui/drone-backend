package com.demo.dronebackend.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContoller {

    private final UserService userService;

    /**
     * 发送验证码
     * @param req 发送验证码请求体
     */
    @PostMapping("/users/send-code")
    public Result<?> sendCode(@Valid @RequestBody SendCodeReq req) {
        return userService.sendCode(req);
    }

    /**
     * 密码登录
     * @param req 密码登录请求体
     */
    @PostMapping("/users/login-pwd")
    public Result loginByPassword(@Valid @RequestBody LoginByPswReq req) {
        return userService.loginByPassword(req);
    }

    /**
     * 验证码登录
     * @param req 验证码登录请求体
     */
    @PostMapping("/users/login-code")
    public Result<?> loginByCode(@Valid @RequestBody LoginByCodeReq req) {
        return userService.loginByCode(req);
    }


    /**
     * 修改密码
     * @param req 修改密码请求体
     */
    @PostMapping("/users/change-pwd")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordReq req) {
        return userService.updatePassword(req);
    }

    /**
     * 忘记密码
     * @param req 重置密码请求体
     */
    @PostMapping("/users/reset-pwd")
    public Result<?> resetPassword(@Valid @RequestBody ResetReq req) {
        return userService.resetPassword(req);
    }

    /**
     * 用户登出
     */
    @GetMapping("/users/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success("登出成功");
    }

    /**
     * 添加人员
     * @param req 添加用户请求体
     */
    @PostMapping("/users")
    public Result<?> addUser(@Valid @RequestBody AddUserReq req) {
        return userService.addUser(req);
    }

    /**
     * 删除人员
     * @param id 用户id
     */
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable("id") @NotBlank String id) {
        return userService.deleteUser(id);
    }

    /**
     * 修改人员信息
     * @param id 用户id
     * @param req 修改用户请求体
     */
    @PutMapping("/users/{id}")
    public Result<?> updateUser(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserReq req) {
        return userService.updateUser(id, req);
    }

    /**
     * 获取人员列表
     * @param req 查询用户列表请求体
     */
    @GetMapping("/users")
    public Result<?> listUsers(@Valid UserQueryReq req) {
        return userService.listUsers(req);
    }

    /**
     * 获取用户名单列表，用于绑定设备
     */
    @GetMapping("/users/available-for-binding")
    public Result<?> userListForBind() {
        return userService.userListForBind();
    }

}
