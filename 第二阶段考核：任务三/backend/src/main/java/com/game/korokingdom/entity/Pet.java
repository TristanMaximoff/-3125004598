package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

//其实这里虽然是精灵，但这个对象对应的是任务中的“职业”一物
//因此存储了当前的关卡数

@Data
@TableName("t_pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;   //对应user.id
    private String name;
    private String petType;   //对应枚举类
    private Integer level;  //等级
    private Integer exp;

    // 基础属性
    private Integer hp;
    private Integer attack;
    private Integer defense;
    private Integer speed;

    private Integer individualValue;  //个体值，0-15保证随机性
    private Integer currentHp;     //战斗中血量，不大于hp
    private Integer currentLevel;  //当前关卡数

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}