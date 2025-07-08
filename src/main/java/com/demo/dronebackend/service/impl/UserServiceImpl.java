package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.dronebackend.dto.admin.*;
import com.demo.dronebackend.enums.PermissionType;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.MyPage;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.pojo.User;
import com.demo.dronebackend.service.UserService;
import com.demo.dronebackend.mapper.UserMapper;
import com.demo.dronebackend.util.CurrentUserContext;
import com.demo.dronebackend.util.MD5Util;
import com.demo.dronebackend.util.SaltUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户Service实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private final UserMapper userMapper;

    @Override
    public Result loginByPassword(LoginRequest req) throws BusinessException {

        User user = this.query().eq("phone", req.getPhone()).one();
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 验证密码（MD5+salt）
        String hashed = MD5Util.hash(req.getPassword(), user.getSalt());
        if (!hashed.equals(user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        // 登录
        StpUtil.login(user.getId());
        // 返回token和permission
        // TODO: 使用LoginDTO
        Map<String,String> map=new HashMap<>();
        map.put("token",StpUtil.getTokenValue());
        map.put("permission",user.getPermission());
        return Result.success(map);
    }

    @Override
    public Result<?> updatePassword(UpdatePasswordReq req) {
        if (req.getNewPassword().equals(req.getOldPassword())){
            return Result.error("新密码不能与原密码相同");
        }
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        String password = user.getPassword();
        if (!MD5Util.hash(req.getOldPassword(), user.getSalt()).equals(password)) {
            return Result.error("旧密码错误");
        }
        // 生成新随机盐并更新
        user.setSalt(SaltUtil.generateSalt());
        user.setPassword(MD5Util.hash(req.getNewPassword(), user.getSalt()));

        userMapper.updateById(user);
        return Result.success("修改密码成功");
    }

    @Override
    public Result<?> resetPassword(ResetReq req) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        // 生成新随机盐并更新
        user.setSalt(SaltUtil.generateSalt());
        user.setPassword(MD5Util.hash(req.getNewPassword(), user.getSalt()));
        userMapper.updateById(user);

        return Result.success("重置密码成功");
    }

    @Override
    public Result<?> addUser(AddUserReq req) {
        if (!isAdmin()){
            throw new BusinessException("无权限");
        }
        boolean valid = verifyCode(req.getPhone(), req.getCode());
        if (!valid) {
            return Result.error("验证码不正确或已过期");
        }
        User user = query().eq("phone", req.getPhone()).one();
        if (user != null) {
            return Result.error("手机号已被注册");
        }
        // 生成随机盐和初始密码
        String salt = SaltUtil.generateSalt();
        String rawPwd = INITIAL_PASSWORD;
        String hashed = MD5Util.hash(rawPwd, salt);
        // 初始化用户信息
        user = new User();
        user.setName(req.getName());
        user.setSex(req.getSex());
        user.setOrganization(req.getOrganization());
        user.setPhone(req.getPhone());
        user.setSalt(salt);
        user.setPassword(hashed);
        user.setPermission(req.getPermission());
        this.save(user);
        return Result.success("添加用户成功");
    }

    @Override
    public Result<?> deleteUser(String id) {
        if (!isAdmin()){
            throw new BusinessException("无权限");
        }
        int cnt = userMapper.deleteById(id);
        if (cnt==0){
            throw new BusinessException("用户不存在");
        }
        return Result.success("删除用户成功");
    }

    @Override
    public Result<?> updateUser(String pathId, UpdateUserReq req) {
        User user = userMapper.selectById(pathId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (StringUtils.hasText(req.getName())) {
            user.setName(req.getName());
        }
        if (StringUtils.hasText(req.getSex())) {
            user.setSex(req.getSex());
        }
        if (StringUtils.hasText(req.getOrganization())) {
            user.setOrganization(req.getOrganization());
        }
        if (StringUtils.hasText(req.getPhone())) {
            user.setPhone(req.getPhone());
        }

        userMapper.updateById(user);
        return Result.success("用户信息更新成功");
    }

    @Override
    public Result<?> listUsers(UserQuery query) {

        Page<User> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            qw.like(User::getName, query.getName());
        }
        if (StringUtils.hasText(query.getPhone())) {
            qw.eq(User::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getPermission())) {
            qw.eq(User::getPermission, query.getPermission());
        }
        User user = CurrentUserContext.get();
        if (!PermissionType.admin.getDesc().equals(user.getPermission())) {
            qw.eq(User::getId, user.getId());
        }
        Page<User> result = userMapper.selectPage(page, qw);
        return Result.success(new MyPage<>(result));
    }

    @Override
    public Result<?> userListForBand() {
        List<User> users = userMapper.selectList(null);
        List<UserListIdNameDto> list = users.stream()
                .map(u -> new UserListIdNameDto(u.getId(), u.getName()))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    // 判断当前用户权限
    private boolean isAdmin(){
        User user = CurrentUserContext.get();
        if(PermissionType.admin.getDesc().equals(user.getPermission())){
            return true;
        }
        return false;
    }
    @Override
    public Result<?> sendCode(SendCodeReq req) {
        String phone = req.getPhone();
        int flag = smsService.sendSms(phone, req.getSign());
        if (flag == -1){
            return Result.error("非法请求");
        }

        return Result.success("验证码发送成功");
    }

    @Override
    public Result<?> loginByCode(LoginByCodeReq req) {
        String phone = req.getPhone();
        String code = req.getCode();
        String storeCode = smsService.getStoredCode( phone);
        if (storeCode == null ){
            return Result.error("验证码已过期");
        }
        if (!storeCode.equals(code)){
            return Result.error("验证码错误");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            return Result.error("用户不存在");
        }
        StpUtil.login(user.getId());
        smsService.deleteCode( phone);
        return Result.success(new LoginDto(StpUtil.getTokenValue(), user.getPermission()));
    }


    //TODO: 验证码校验逻辑
    private boolean verifyCode(String phone, String code) {
        return true;
    }
}




