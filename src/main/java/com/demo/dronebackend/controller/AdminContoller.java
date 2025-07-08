package com.demo.dronebackend.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminContoller {

    private final UserService userService;

    /**
     * 密码登录
     * @param req 登录请求体
     * @return
     */
    @PostMapping("/users/login-pwd")
    public Result loginByPassword(@Valid @RequestBody LoginByPswReq req) {
        return userService.loginByPassword(req);
    }

    /**
     * 修改密码
     */
    @PostMapping("/users/change-pwd")
    public Result<?> updatePassword(@Valid @RequestBody UpdatePasswordReq req) {
        return userService.updatePassword(req);
    }

    /**
     * 忘记密码
     *
     * @param req
     * @return
     */
    @PostMapping("/users/reset-pwd")
    public Result<?> resetPassword(@Valid @RequestBody ResetReq req) {
        return userService.resetPassword(req);
    }

    @GetMapping("/users/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success("登出成功");
    }

    /**
     * 添加人员
     *
     * @param req
     * @return
     */
    @PostMapping("/users")
    public Result<?> addUser(@Valid @RequestBody AddUserReq req) {
        return userService.addUser(req);
    }

    /**
     * 删除人员
     */
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable("id") String pathId) {
        return userService.deleteUser(pathId);
    }

    /*
    修改人员信息
     */
    @PutMapping("/users/{id}")
    public Result<?> updateUser(
            @PathVariable("id") String pathId,
            @Valid @RequestBody UpdateUserReq req) {
        return userService.updateUser(pathId, req);
    }

    /**
     * 获取人员列表
     */
    @GetMapping("/users")
    public Result<?> listUsers(UserQuery query) {
        return userService.listUsers(query);
    }

    @GetMapping("/available-for-binding")
    public Result<?> userListForBand() {
        return userService.userListForBand();
    }
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
