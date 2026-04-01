package com.itheima.dormrepairsystemspringboot.service;
import com.itheima.dormrepairsystemspringboot.pojo.Repair;
import java.util.List;

public interface RepairService {

    void addRepair(Repair repair);

    List<Repair> myRepairs(String account);

    void cancel(Integer id);

    List<Repair> allRepairs();

    void updateStatus(Integer id, String status);

    void delete(Integer id);

    Repair getById(Integer id);
}