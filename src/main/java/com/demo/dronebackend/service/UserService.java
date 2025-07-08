package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service
 */

public interface UserService extends IService<User> {

    Result loginByPassword(LoginByPswReq req) throws BusinessException;

    Result<?> addUser(AddUserReq req);

    Result<?> updatePassword(UpdatePasswordReq req);

    Result<?> resetPassword(ResetReq req);

    Result<?> deleteUser(String pathId);

    Result<?> updateUser(String pathId, UpdateUserReq req);

    Result<?> listUsers(UserQuery query);

    Result<?> userListForBand();

    Result<?> sendCode(SendCodeReq req);

    Result<?> loginByCode(LoginByCodeReq req);
}
