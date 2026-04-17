package com.game.korokingdom.entity;

import lombok.Data;

@Data
public class Skill {
    private Integer id;
    private String name;
    private String skillType;     // ATTACK/DEFENSE/STATUS —— 技能系别默认和pet一样
    //private Integer power;        // 威力（攻击技能用），目前伤害公式里没用到此系数，注释掉了啦啦啦~
    private Integer maxPp;        // 最大使用次数（单次关卡）
    private String description;     //技能效果，展示在前端

    private Integer buffTarget;   // 增益目标：1=攻击提升，2=防御提升
    private Double buffValue;     // 增益数值：1.0=提升100%
    private Double damageReduction; // 伤害减免：0.9=减免90%
    private Double reflectRate;   // 反伤比例：0.5=反伤50%
    private Double healRate;      // 回血比例：0.3=回复30%最大生命
}