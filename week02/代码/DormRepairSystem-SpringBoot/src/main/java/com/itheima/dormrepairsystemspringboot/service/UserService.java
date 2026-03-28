package com.itheima.dormrepairsystemspringboot.service;
import com.itheima.dormrepairsystemspringboot.pojo.User;

public interface UserService {

    String register(User user);

    User login(String account, String password);

    void updateUser(User user);
}