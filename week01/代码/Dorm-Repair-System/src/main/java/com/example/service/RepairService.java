package com.example.service;
import com.example.entity.Repair;
import com.example.mapper.RepairMapper;
import java.util.List;

//RepairMapper把RepairService和数据库连在一起
public class RepairService {   //RepairService对象相应的方法
    private final RepairMapper repairMapper;

    public RepairService(RepairMapper repairMapper) {
        this.repairMapper = repairMapper;
    }//把 RepairMapper 注入进来，让后面的方法能调用数据库操作

    //提交报修
    public void addRepair(Repair repair){
        repairMapper.addRepair(repair);
    }
    //查询我的报修
    public List<Repair> myRepairs(String account){
        return repairMapper.findByAccount(account);
    }
    //取消报修
    public void cancel(Integer id){
        repairMapper.cancelRepair(id);
    }
    //管理员：查全部
    public List<Repair> allRepairs(){
        return repairMapper.findAll();
    }
    //管理员：改状态
    public void updateStatus(Integer id, String status){
        repairMapper.updateStatus(id, status);
    }
    // 管理员：删除
    public void delete(Integer id){
        repairMapper.deleteRepair(id);
    }
}