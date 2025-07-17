package com.demo.dronebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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
import com.demo.dronebackend.util.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.demo.dronebackend.constant.SystemConstants.INITIAL_PASSWORD;

/**
 * 用户Service实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private final UserMapper userMapper;
    private final SmsService smsService;

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
    public Result loginByPassword(LoginByPswReq req) throws BusinessException {
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
        // 返回token，permission以及无人值守状态
        LoginDTO loginDTO = new LoginDTO(StpUtil.getTokenValue(), user.getPermission(), user.getUnattended());
        return Result.success(loginDTO);
    }

    @Override
    public Result<?> loginByCode(LoginByCodeReq req) {
        String phone = req.getPhone();
        String code = req.getCode();
        String storeCode = smsService.getStoredCode(phone);
        if (storeCode == null ){
            return Result.error("验证码已过期");
        }
        if (!storeCode.equals(code)){
            return Result.error("验证码错误");
        }
        User user = query().eq("phone", phone).one();
        if (user == null) {
            return Result.error("用户不存在");
        }
        StpUtil.login(user.getId());
        smsService.deleteCode(phone);
        // 返回token，permission以及无人值守状态
        LoginDTO loginDTO = new LoginDTO(StpUtil.getTokenValue(), user.getPermission(), user.getUnattended());
        return Result.success(loginDTO);
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
        String storeCode = smsService.getStoredCode(req.getPhone());
        if (storeCode == null ){
            return Result.error("验证码已过期");
        }
        if (!storeCode.equals(req.getCode())){
            return Result.error("验证码错误");
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
        int cnt = userMapper.deleteById(Long.parseLong(id));
        if (cnt==0){
            throw new BusinessException("用户不存在");
        }
        return Result.success("删除用户成功");
    }

    @Override
    public Result<?> updateUser(String id, UpdateUserReq req) {
        if (!isAdmin()){
            throw new BusinessException("无权限");
        }
        User user = userMapper.selectById(Long.parseLong(id));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StrUtil.isNotBlank(req.getName())){
            user.setName(req.getName());
        }
        if (StrUtil.isNotBlank(req.getSex())){
            user.setSex(req.getSex());
        }
        if (StrUtil.isNotBlank(req.getPhone())){
            user.setPhone(req.getPhone());
        }
        if (StrUtil.isNotBlank(req.getOrganization())){
            user.setOrganization(req.getOrganization());
        }
        if (StrUtil.isNotBlank(req.getPermission())){
            user.setPermission(req.getPermission());
        }

        userMapper.updateById(user);
        return Result.success("用户信息更新成功");
    }

    @Override
    public Result<?> listUsers(UserQueryReq req) {
        if (!isAdmin()){
            throw new BusinessException("无权限");
        }
        System.out.println(req.getPhone());
        Page<User> page = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(req.getName())) {
            qw.like(User::getName, req.getName());
        }
        if (StrUtil.isNotBlank(req.getPhone())) {
            qw.eq(User::getPhone, req.getPhone());
        }
        if (StrUtil.isNotBlank(req.getPermission())) {
            qw.eq(User::getPermission, req.getPermission());
        }
        Page<User> result = userMapper.selectPage(page, qw);
        List<UserDTO> dtoList = result.getRecords()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        MyPage<UserDTO> dtoPage = new MyPage<>(result);
        dtoPage.setRecords(dtoList);
        return Result.success(dtoPage);
    }

    @Override
    public Result<?> userListForBind() {
        List<User> users = query().list();
        List<UsersBindDTO> list = users.stream()
                .map(u -> new UsersBindDTO(u.getId(), u.getName()))
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @Override
    public Result<?> setUnattended(Boolean flag) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (flag){
            user.setUnattended(1);
        }else {
            user.setUnattended(0);
        }
        userMapper.updateById(user);
        return Result.success("设置成功");
    }

    // 判断当前用户权限
    private boolean isAdmin(){
        User user = CurrentUserContext.get();
        if(PermissionType.admin.getDesc().equals(user.getPermission())){
            return true;
        }
        return false;
    }
}




