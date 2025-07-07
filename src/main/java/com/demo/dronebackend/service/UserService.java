package com.demo.dronebackend.service;

import com.demo.dronebackend.dto.LoginRequest;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 28611
* @description 针对表【user(人员表)】的数据库操作Service
* @createDate 2025-07-07 09:44:52
*/
public interface UserService extends IService<User> {

    Result loginByPassword(LoginRequest req) throws BusinessException;
}
