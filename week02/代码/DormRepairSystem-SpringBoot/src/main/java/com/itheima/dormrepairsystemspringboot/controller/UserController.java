package com.itheima.dormrepairsystemspringboot.controller;

import com.itheima.dormrepairsystemspringboot.common.Result;
import com.itheima.dormrepairsystemspringboot.pojo.User;
import com.itheima.dormrepairsystemspringboot.service.UserService;
import com.itheima.dormrepairsystemspringboot.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    //用户注册
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        return userService.register(user); //这个方法返回Result类
    }


    //用户登录--从前端传一个账号密码的json数据到后端再作比较，所以是POST不是GET
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User loginUser = userService.login(user.getAccount(), user.getPassword());
        if (loginUser == null) {
            return Result.error("账号或密码错误");
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getId());
        claims.put("account", loginUser.getAccount());
        claims.put("role", loginUser.getRole());
        String token = JwtUtil.generateToken(claims);

        // ========== 关键修改：返回包含token和user的Map ==========
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("token", token);
        resultData.put("user", loginUser); // 把用户信息一起返回，这里增添了id、dormDirection、status等

        return Result.success(resultData);
    }


    //修改用户信息（绑定宿舍/改密码）
    @PutMapping
    public Result updateUser(@RequestBody User user) {
        userService.updateUser(user);
        return Result.success("宿舍绑定成功");
    }

    //
    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return Result.success(user);
    }
}