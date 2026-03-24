package com.example.mapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {
    @Select("SELECT * FROM user WHERE account = #{account}")
    User findByAccount(String account);

    @Insert("INSERT INTO user(account,password,role) VALUES(#{account},#{password},#{role})")
    int addUser(User user);

    @Update("UPDATE user SET password=#{password}, dormDirection=#{dormDirection}, dormBuilding=#{dormBuilding}, dormRoom=#{dormRoom}, dormStatus=#{dormStatus} WHERE id=#{id}")
    int updateUser(User user);
}