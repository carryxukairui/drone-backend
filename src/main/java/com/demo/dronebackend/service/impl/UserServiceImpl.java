package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.LoginRequest;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.UserService;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.util.MD5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author 28611
* @description 针对表【user(人员表)】的数据库操作Service实现
* @createDate 2025-07-07 09:44:52
*/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private final UserMapper userMapper;

    @Override
    public Result loginByPassword(LoginRequest req) throws BusinessException {

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 1. 验证密码（MD5+salt）
        String hashed = MD5Util.hash(req.getPassword(), user.getSalt());
        if (!hashed.equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        StpUtil.login(user.getId());

        return Result.success(StpUtil.getTokenValue());
    }
}




