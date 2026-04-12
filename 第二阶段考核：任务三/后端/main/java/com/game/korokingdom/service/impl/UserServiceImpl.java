package com.game.korokingdom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.game.korokingdom.dto.request.LoginReq;
import com.game.korokingdom.dto.request.RegisterReq;
import com.game.korokingdom.dto.response.UserResp;
import com.game.korokingdom.entity.User;
import com.game.korokingdom.exception.BusinessException;
import com.game.korokingdom.mapper.UserMapper;
import com.game.korokingdom.service.UserService;
import com.game.korokingdom.utils.JwtUtil;
import com.game.korokingdom.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    @Override
    public String register(RegisterReq req) {
        // 校验密码和确认密码是否一致
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 校验用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, req.getUsername());
        if (this.baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 校验邮箱是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, req.getEmail());
        if (this.baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordUtil.encode(req.getPassword()));
        user.setNickname(req.getUsername()); // 默认昵称为用户名
        user.setCreatedAt(LocalDateTime.now());

        this.baseMapper.insert(user);

        // 生成token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public String login(LoginReq req) {
        // 根据邮箱或手机号查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, req.getAccount())
                .or()
                .eq(User::getPhone, req.getAccount());

        User user = this.baseMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 校验密码
        if (!passwordUtil.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }

        // 生成token
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public UserResp getUserInfo(Long userId) {
        User user = this.baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(user.getAvatarUrl());
        return resp;
    }

    @Override
    public void updateNickname(Long userId, String nickname) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setUpdatedAt(LocalDateTime.now());
        this.baseMapper.updateById(user);
    }
}