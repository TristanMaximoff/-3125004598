package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class RegisterReq {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
}