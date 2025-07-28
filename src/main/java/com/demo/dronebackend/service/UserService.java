package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.util.Result;
import com.demo.dronebackend.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service
 */

public interface UserService extends IService<User> {

    Result<?> sendCode(SendCodeReq req);

    Result loginByPassword(LoginByPswReq req) throws BusinessException;

    Result<?> loginByCode(LoginByCodeReq req);

    Result<?> updatePassword(UpdatePasswordReq req);

    Result<?> resetPassword(ResetReq req);

    Result<?> addUser(AddUserReq req);

    Result<?> deleteUser(String id);

    Result<?> updateUser(String id, UpdateUserReq req);

    Result<?> listUsers(UserQueryReq req);

    Result<?> userListForBind();

    Result<?> setUnattended(Boolean flag);
}
