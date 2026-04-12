package com.game.korokingdom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.game.korokingdom.entity.User;
import com.game.korokingdom.dto.request.RegisterReq;
import com.game.korokingdom.dto.request.LoginReq;
import com.game.korokingdom.dto.response.UserResp;

public interface UserService extends IService<User> {

    // 注册
    String register(RegisterReq req);

    // 登录
    String login(LoginReq req);

    // 获取用户信息
    UserResp getUserInfo(Long userId);

    // 修改昵称
    void updateNickname(Long userId, String nickname);
}