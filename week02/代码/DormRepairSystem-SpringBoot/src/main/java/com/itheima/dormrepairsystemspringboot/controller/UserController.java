package com.itheima.dormrepairsystemspringboot.controller;

import com.itheima.dormrepairsystemspringboot.pojo.User;
import com.itheima.dormrepairsystemspringboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    //用户注册
    @PostMapping
    public String register(@RequestBody User user) {
        return userService.register(user);
    }
    //用户登录--从前端传一个账号密码的json数据到后端再作比较，所以是POST不是GET
    @PostMapping("/login")
    public User login(@RequestBody User user) {
        return userService.login(user.getAccount(), user.getPassword());
    }


    //修改用户信息（绑定宿舍/改密码）
    @PutMapping
    public void updateUser(@RequestBody User user) {
        userService.updateUser(user);
    }
}