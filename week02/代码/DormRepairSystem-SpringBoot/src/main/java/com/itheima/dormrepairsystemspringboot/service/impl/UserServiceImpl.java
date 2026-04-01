package com.itheima.dormrepairsystemspringboot.service.impl;
import com.itheima.dormrepairsystemspringboot.common.Result;
import com.itheima.dormrepairsystemspringboot.mapper.UserMapper;
import com.itheima.dormrepairsystemspringboot.pojo.User;
import com.itheima.dormrepairsystemspringboot.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper; //final使其只能被赋值一次，避免被意外修改
    public UserServiceImpl(UserMapper userMapper){
        this.userMapper = userMapper;
    }//调用mapper层

    //注册——SELECT
    //接受一个user对象，检查重复、账号开头后返回注册结果（字符串）
    public Result register(User user){
        User exist = userMapper.findByAccount(user.getAccount());

        if(exist != null) return Result.error("账号已存在");
        if("student".equals(user.getRole())){
            if(!user.getAccount().startsWith("3125") && !user.getAccount().startsWith("3225")){
                return Result.error("学生账号必须以3125/3225开头");
            }
        }else{
            if(!user.getAccount().startsWith("0025")){
                return Result.error("管理员账号必须以0025开头");
            }
        }
        userMapper.addUser(user);
        return Result.success("注册成功"); // 返回Result
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

    // 根据id查询用户
    @Override
    public User getById(Integer id) {
        return userMapper.findById(id);
    }
}