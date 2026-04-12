package com.game.korokingdom.entity;

import lombok.Data;

@Data
public class Skill {
    private Integer id;
    private String name;
    private String skillType;     // ATTACK / DEFENSE / STATUS
    private Integer power;        // 威力（攻击技能用）
    private Integer maxPp;        // 最大使用次数
    private String description;
    private Integer buffTarget;   // 增益目标：1=攻击提升，2=防御提升
    private Double buffValue;     // 增益数值：1.0=提升100%
    private Double damageReduction; // 伤害减免：0.9=减免90%
    private Double reflectRate;   // 反伤比例：0.5=反伤50%
    private Double healRate;      // 回血比例：0.3=回复30%最大生命
}