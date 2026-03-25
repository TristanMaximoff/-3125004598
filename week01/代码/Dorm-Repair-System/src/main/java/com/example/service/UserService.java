package com.example.service;
import com.example.entity.User;
import com.example.mapper.UserMapper;

public class UserService {

    private final UserMapper userMapper; //final使其只能被赋值一次，避免被意外修改
    public UserService(UserMapper userMapper){
        this.userMapper = userMapper;
    }//调用mapper层

    //注册——SELECT
    //接受一个user对象，检查重复、账号开头后返回注册结果（字符串）
    public String register(User user){
        User exist = userMapper.findByAccount(user.getAccount());

        if(exist != null) return "账号已存在";
        if("student".equals(user.getRole())){
            if(!user.getAccount().startsWith("3125") && !user.getAccount().startsWith("3225")){
                return "学生账号必须以3125/3225开头";
            }
        }else{
            if(!user.getAccount().startsWith("0025")){
                return "管理员账号必须以0025开头";
            }
        }
        //若全都合规则调用mapper录入用户
        //INSERT
        userMapper.addUser(user);
        return "注册成功";
    }

    //登录
    public User login(String account, String password){
        User user = userMapper.findByAccount(account);
        if(user == null) return null;                         //找不到账户
        if(!user.getPassword().equals(password)) return null; //密码不匹配
        else return user;
    }

    //修改信息/密码——UPDATE
    public void updateUser(User user){
        userMapper.updateUser(user);
    }
}