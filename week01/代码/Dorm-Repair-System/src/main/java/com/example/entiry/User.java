package com.example.entity;
import lombok.Data;
import java.time.LocalDateTime;

//@data即包含了setter和getter的方法
@Data

public class User {
    private Integer id;
    private String account;
    private String password;
    private String role;
    //"student"|"admin"
    private String dormDirection;
    // "东"|“西”
    private Integer dormBuilding;
    // 第几栋 1-10
    private Integer dormRoom;
    // 101-610
    private Integer dormStatus;
    /*用于判断是否是初次登录，
    0未绑定 1已绑定 */
}