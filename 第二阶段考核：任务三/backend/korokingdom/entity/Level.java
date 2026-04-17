package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_level")
public class Level {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer levelNum;  //关卡编号
    private String name;
    private String monsterName;
    private String monsterType;   //系别
    private Integer monsterHp;
    private Integer monsterAttack;
    private Integer monsterDefense;
    private Integer expReward;
    private Integer firstExpBonus;   //首次通关额外经验奖励
}