package com.example.entity;
import lombok.Data; //自动写入getter、setter等功能

@Data
//代表一个“存储一个用户（学生/管理员）”的类
public class User {
    private Integer id;
    private String account;
    private String password;
    private String role;//"student"|"admin"
    private String dormDirection;// "东"|“西”
    private Integer dormBuilding;// 第几栋 1-10
    private Integer dormRoom;// 101-610
    private Integer dormStatus;//用于判断是否是初次登录，0未绑定 1已绑定
}