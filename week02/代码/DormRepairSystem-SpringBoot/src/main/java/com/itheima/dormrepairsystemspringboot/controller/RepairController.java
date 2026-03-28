package com.itheima.dormrepairsystemspringboot.controller;

import com.itheima.dormrepairsystemspringboot.pojo.Repair;
import com.itheima.dormrepairsystemspringboot.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //把当前类变成一个接口控制器，所有方法返回值直接作为 JSON 数据返回给前端
@RequestMapping("/repairs")
public class RepairController {

    @Autowired
    private RepairService repairService;

    //查询我的报修
    @GetMapping("/my/{account}")
    public List<Repair> myRepairs(@PathVariable String account) {  //@PatheVariable -- 把{key}传给后面的key变量
        return repairService.myRepairs(account);                   //Path传单个值，RequestBody传多个值/对象，但都是前端接受了数据后操作的
    }
    // 查询所有报修
    @GetMapping
    public List<Repair> allRepairs() {
        return repairService.allRepairs();
    }


    //提交报修  POST/repairs/
    @PostMapping
    public void addRepair(@RequestBody Repair repair) {   //拿请求体里JSON类型的数据
        repairService.addRepair(repair);
    }


    //修改报修状态
    @PutMapping("/{id}")
    public void updateStatus(@PathVariable Integer id, @RequestParam String status) {
        repairService.updateStatus(id, status);
    }
    //取消报修（实则为修改状态status）
    @PutMapping("/cancel/{id}")
    public void cancel(@PathVariable Integer id) {
        repairService.cancel(id);
    }

    // 删除报修
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        repairService.delete(id);
    }
}