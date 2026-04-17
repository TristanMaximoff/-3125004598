package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;



@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)  //把id设为主键
    private Long id;

    private String username;
    private String email;
    private String phone;
    private String passwordHash;//哈希加密，加密后的密文不唯一但都能match，保证无法从后端获取密码
    private String nickname;   //游戏内名字，非登录用
    private String avatarUrl;  //头像地址

    @TableField(fill = FieldFill.INSERT) //自动设置值
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}