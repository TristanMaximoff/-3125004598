package com.itheima.dormrepairsystemspringboot.service;
import com.itheima.dormrepairsystemspringboot.common.Result;
import com.itheima.dormrepairsystemspringboot.pojo.User;

public interface UserService {

    Result register(User user);

    User login(String account, String password);

    void updateUser(User user);

    User getById(Integer id);
}