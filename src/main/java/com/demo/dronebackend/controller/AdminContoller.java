package com.demo.dronebackend.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/users")
@RequiredArgsConstructor
public class AdminContoller {

    private final UserService userService;

    /**
      密码登录
     */
    @PostMapping("/login-pwd")
    public Result loginByPassword( @Valid @RequestBody LoginRequest req) {
        return userService.loginByPassword(req);
    }

    /**
     * 添加人员
     */
    @PostMapping()
    public Result<?> addUser(@Valid @RequestBody AddUserReq req) {
        return  userService.addUser(req);
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
     * 删除人员
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable("id") String pathId) {
        return  userService.deleteUser(pathId);
    }

    /*
    修改人员信息
     */
    @PutMapping("/{id}")
    public Result<?> updateUser(
            @PathVariable("id") String pathId,
            @Valid @RequestBody UpdateUserReq req) {
        return userService.updateUser(pathId, req);
    }

    /**
     * 获取人员列表
     */
    @GetMapping()
    public Result<?> listUsers(UserQuery query) {
        return userService.listUsers(query);
    }

    @GetMapping("/userListForBand")
    public Result<?> userListForBand() {
        return userService.userListForBand();
    }

    @GetMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success("登出成功");
    }



}
