package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class LoginReq {
    private String account;   // 邮箱或手机号
    private String password;
}