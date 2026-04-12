package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class BattleActionReq {
    private String actionType;   // ATTACK / SKILL / SURRENDER
    private Integer skillId;     // 技能ID（当actionType为SKILL时使用）
    private Integer roundSeq;    // 回合序列号
}