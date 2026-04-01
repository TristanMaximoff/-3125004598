package com.itheima.dormrepairsystemspringboot.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
//Repair是一个存储“一项修理请求”的类
public class Repair {

    public Repair() {}

    private Integer id;//和user的id不一样
    //使用Integer对应数据库中的int，Integer可以存储未赋值时的null，int未赋值时默认为0，存入数据库可能导致歧义（例如用0/1表示状态时）
    private String studentAccount;//对应user的account
    private String deviceType;//设备类型
    private String description;//设备问题
    private String imageUrl; // 新增：图片访问地址
    private String status;//“待处理”|“处理中”|“已完成”
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
