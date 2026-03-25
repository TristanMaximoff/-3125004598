package com.example.mapper;
import com.example.entity.Repair;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface RepairMapper {

    @Insert("INSERT INTO repair(student_account,device_type,description,status) VALUES(#{studentAccount},#{deviceType},#{description},#{status})")
    int addRepair(Repair repair);
    //和addUser一个作用
    //id数据被设置为主键和自增，会根据数据数自动增加填入不用手动填入

    @Select("SELECT * FROM repair WHERE studentAccount = #{account}")
    List<Repair> findByAccount(String account);
    //一个用户可以对应多条报修记录，返回N个Repair对象

    @Select("SELECT * FROM repair")
    List<Repair> findAll();

    @Update("UPDATE repair SET status=#{status}, update_time=now() WHERE id=#{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
    //因为main里只传id和status
    //把后面的参数和前面括号内的数据库数据名对应

    @Update("UPDATE repair SET status='已取消' WHERE id=#{id}")
    int cancelRepair(Integer id);
    //更新状态status

    //删数据
    @Delete("DELETE FROM repair WHERE id=#{id}")
    int deleteRepair(Integer id);


}