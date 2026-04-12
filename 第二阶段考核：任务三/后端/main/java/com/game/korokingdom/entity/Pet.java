package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String petType;
    private Integer level;
    private Integer exp;

    // 基础属性
    private Integer hp;
    private Integer attack;
    private Integer defense;
    private Integer speed;

    private Integer individualValue;
    private Integer currentHp;
    private Integer currentLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}