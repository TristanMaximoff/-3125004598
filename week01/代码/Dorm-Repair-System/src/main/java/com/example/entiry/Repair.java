package com.example.entity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Repair {
    private Integer id;
    //和user的id不一样
    private String studentAccount;
    //对应user的account
    private String deviceType;
    private String description;
    private String status;
    //“待处理”|“处理中”|“已完成”
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}