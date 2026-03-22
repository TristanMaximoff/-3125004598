package com.example.service;
import com.example.entity.User;
import com.example.mapper.UserMapper;

public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 注册
    public String register(User user) {
        User exist = userMapper.findByAccount(user.getAccount());
        if (exist != null) return "账号已存在";

        // 学生账号校验
        if ("student".equals(user.getRole())) {
            if (!user.getAccount().startsWith("3125") && !user.getAccount().startsWith("3225")) {
                return "学生账号必须以3125/3225开头";
            }
        } else {
            if (!user.getAccount().startsWith("0025")) {
                return "管理员账号必须以0025开头";
            }
        }

        userMapper.addUser(user);
        return "注册成功";
    }

    // 登录
    public User login(String account, String password) {
        User user = userMapper.findByAccount(account);
        if (user == null) return null;
        if (!user.getPassword().equals(password)) return null;
        return user;
    }

    // 修改信息/密码
    public void updateUser(User user) {
        userMapper.updateUser(user);
    }
}