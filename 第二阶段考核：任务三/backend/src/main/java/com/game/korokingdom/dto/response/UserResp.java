package com.game.korokingdom.dto.response;

import lombok.Data;

@Data
public class UserResp {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
}