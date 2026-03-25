package com.example.mapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {
    @Select("SELECT * FROM user WHERE account = #{account}")
    User findByAccount(String account);
    //传入账号的参数，返回账号对应的用户对象

    @Insert("INSERT INTO user(account,password,role) VALUES(#{account},#{password},#{role})")
    int addUser(User user);
    //传入user的账户、密码、角色存入数据库，返回受影响行数，1成功0失败

    @Update("UPDATE user SET password=#{password}, dormDirection=#{dormDirection}, dormBuilding=#{dormBuilding}, dormRoom=#{dormRoom}, dormStatus=#{dormStatus} WHERE id=#{id}")
    int updateUser(User user);
    //更新用户数据，密码/宿舍方位/宿舍栋数/宿舍门牌号/登录状态/
    //只更改对应编号的数据
}

//接口创建对象的流程：
//SqlSession sqlSession = MyBatisUtil.getSqlSession();
//UserMapper mapper = sqlSession.getMapper(UserMapper.class);
//调用方法：User user = mapper.findByAccount("3125001");