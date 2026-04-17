package com.game.korokingdom.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

//pring Security 框架提供的类和方法
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    //raw-前端原密码，encoded-数据库中保存的加密后的密码
    // 加密密码
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 校验密码：原始密码 vs 加密后的密码
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}