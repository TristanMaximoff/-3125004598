package com.itheima.dormrepairsystemspringboot.controller;

import com.itheima.dormrepairsystemspringboot.common.Result;
import com.itheima.dormrepairsystemspringboot.pojo.Repair;
import com.itheima.dormrepairsystemspringboot.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController //把当前类变成一个接口控制器，所有方法返回值直接作为 JSON 数据返回给前端
@RequestMapping("/repairs")
public class RepairController {

    @Autowired
    private RepairService repairService;

    @Value("${upload.path}")   //把配置文件中读取upload.path的值赋给uploadPath
    private String uploadPath;  //读取图片保存路径

    @Value("${upload.url-prefix}")
    private String urlPrefix;   //读取图片访问URL前缀

    //查询我的报修
    @GetMapping("/my/{account}")
    public Result myRepairs(@PathVariable String account) {
        return Result.success(repairService.myRepairs(account));
    }
    //查询所有报修
    @GetMapping
    public Result allRepairs() {
        return Result.success(repairService.allRepairs());
    }
    //查询第几个报修信息，具体在点击某个报修单的“详情”的时候调用
    @GetMapping("/{id}")
    public Result getRepairById(@PathVariable Integer id) {
        Repair repair = repairService.getById(id);
        return Result.success(repair);
    }


    //提交报修  POST
    @PostMapping
    public Result addRepair(@RequestBody Repair repair) {   //拿请求体里JSON类型的数据
        if (repair.getStatus() == null || repair.getStatus().isEmpty()) {  //默认状态为待处理：前端发送POST请求后后端自动注入待处理了的状态
            repair.setStatus("待处理");
        }
        repairService.addRepair(repair);
        return Result.success("报修提交成功");
    }
    //上传图片使用配置文件路径
    @PostMapping("/upload")
    public Result uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();           //原始名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")); //提取后缀
        String fileName = UUID.randomUUID() + suffix;   //生成全球唯一的随机字符串

        File uploadDir = new File(uploadPath);  //从配置文件读取路径，创建上传目录
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        try {
            file.transferTo(new File(uploadPath + fileName));   //把上传的文件保存到指定位置
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }

        String imageUrl = urlPrefix + fileName;   //从配置文件读取，生成访问地址，返回前端，供前端查看时访问
        return Result.success(imageUrl);
    }


    //修改报修状态
    @PutMapping("/{id}")
    public Result updateStatus(@PathVariable Integer id, @RequestParam String status) {
        repairService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
    //取消报修（实则为修改状态status）
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Integer id) {
        repairService.cancel(id);
        return Result.success("已取消");
    }


    //删除报修
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        repairService.delete(id);
        return Result.success("删除成功");
    }




}