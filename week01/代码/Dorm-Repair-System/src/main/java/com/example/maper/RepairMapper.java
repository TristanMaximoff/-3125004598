package com.example.mapper;
import com.example.entity.Repair;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface RepairMapper {

    //加数据
    @Insert("INSERT INTO repair(student_account,device_type,description,status) VALUES(#{studentAccount},#{deviceType},#{description},#{status})")
    int addRepair(Repair repair);

    //查数据
    @Select("SELECT * FROM repair WHERE studentAccount = #{account}")
    List<Repair> findByAccount(String account);

    @Select("SELECT * FROM repair")
    List<Repair> findAll();

    //改数据
    @Update("UPDATE repair SET status=#{status}, update_time=now() WHERE id=#{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE repair SET status='已取消' WHERE id=#{id}")
    int cancelRepair(Integer id);

    //删数据
    @Delete("DELETE FROM repair WHERE id=#{id}")
    int deleteRepair(Integer id);


}