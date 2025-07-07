package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.user.AddUserRequest;
import com.demo.dronebackend.dto.user.LoginRequest;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.UserService;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.util.MD5Util;
import com.demo.dronebackend.util.SaltUtil;
import com.demo.dronebackend.util.SnowflakeIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

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

    @Override
    public Result<?> addUser(AddUserRequest req) {
        boolean valid = verifyCode(req.getPhone(), req.getCode());
        if (!valid) {
            throw new BusinessException("验证码不正确或已过期");
        }

        User selectOne = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (selectOne != null) {
            throw new BusinessException("手机号已被注册");
        }

        // 3. 生成随机盐和初始密码（这里默认“123456”，业务可调整）
        String salt = SaltUtil.generateSalt();
        String rawPwd = "123456";
        String hashed = MD5Util.hash(rawPwd, salt);


        User user = new User();
        user.setId(SnowflakeIdUtil.INSTANCE.nextId());
        user.setName(req.getName());
        user.setSex(req.getSex());
        user.setOrganization(req.getOrganization());
        user.setPhone(req.getPhone());
        user.setSalt(salt);
        user.setPassword(hashed);
        user.setPermission("user");
        userMapper.insert(user);

        return Result.success("添加用户成功");
    }


    //TODO: 验证码校验逻辑
    private boolean verifyCode(String phone, String code) {
        return true;
    }
}




