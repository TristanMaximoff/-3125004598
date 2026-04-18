package com.game.korokingdom.utils;

import org.springframework.stereotype.Component;

@Component
public class DamageCalculator {

    // 系别克制系数：克制+0.5，被克制-0.5，正常0
    // 最终系数 = 1 + 克制系数
    public double getTypeModifier(String attackType, String defendType) {
        if (attackType == null || defendType == null) {
            return 1.0;
        }

        // 火克草
        if (attackType.equals("FIRE") && defendType.equals("GRASS")) {
            return 1.5;
        }
        // 草克水
        if (attackType.equals("GRASS") && defendType.equals("WATER")) {
            return 1.5;
        }
        // 水克火
        if (attackType.equals("WATER") && defendType.equals("FIRE")) {
            return 1.5;
        }
        // 被克制
        if (attackType.equals("GRASS") && defendType.equals("FIRE")) {
            return 0.5;
        }
        if (attackType.equals("WATER") && defendType.equals("GRASS")) {
            return 0.5;
        }
        if (attackType.equals("FIRE") && defendType.equals("WATER")) {
            return 0.5;
        }
        return 1.0;
    }

    // 伤害公式：10 * ln(攻击 - 防御) * (1 + 等级差 * 1%) * 系别克制系数
    // 保底伤害：1点
    public int calculateDamage(int attackerAttack, int defenderDefense,
                               String attackerType, String defenderType,
                               int attackerLevel, int defenderLevel) {

        // 攻击减防御，最小值设为1（ln1=0，会导致伤害为0，所以保底后面处理）
        int atkMinusDef = attackerAttack - defenderDefense;
        if (atkMinusDef < 1) {
            atkMinusDef = 1;
        }

        // ln(攻击-防御)
        double lnValue = Math.log(atkMinusDef);

        // 等级差影响：1 + 等级差 * 1%
        int levelDiff = attackerLevel - defenderLevel;
        double levelModifier = 1.0 + levelDiff * 0.01;

        // 系别克制系数
        double typeModifier = getTypeModifier(attackerType, defenderType);

        // 最终伤害
        int finalDamage = (int) (10 * lnValue * levelModifier * typeModifier);

        // 保底伤害1点
        if (finalDamage < 1) {
            finalDamage = 1;
        }

        return finalDamage;
    }
}