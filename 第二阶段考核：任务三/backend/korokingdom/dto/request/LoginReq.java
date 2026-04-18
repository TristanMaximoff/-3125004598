package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class LoginReq {
    private String account;   // 邮箱、手机号、用户名
    private String password;
}