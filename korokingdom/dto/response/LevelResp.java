package com.game.korokingdom.dto.response;

import lombok.Data;

//实际和level没区别，只是保持规范
@Data
public class LevelResp {
    private Integer id;
    private Integer levelNum;
    private String name;
    private String monsterName;
    private String monsterType;
    private Integer monsterHp;
    private Integer monsterAttack;
    private Integer monsterDefense;
    private Integer expReward;
    private Integer firstExpBonus;
}