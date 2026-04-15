package com.game.korokingdom.dto;

import com.game.korokingdom.entity.Skill;
import lombok.Data;

@Data
public class BattleSkillState {
    private Skill skill;
    private int currentPp;  // 当前剩余次数
}