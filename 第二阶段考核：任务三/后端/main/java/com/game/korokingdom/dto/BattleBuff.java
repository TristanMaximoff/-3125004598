package com.game.korokingdom.dto;

import lombok.Data;

@Data
public class BattleBuff {
    private double attackBoost;   // 攻击提升比例（1.0=提升100%）
    private double defenseBoost;  // 防御提升比例（0.5=提升50%）
}