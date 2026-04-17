package com.game.korokingdom.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BattleResp {
    private Long battleId;
    private String status;
    private Integer currentRound;

    private Long petId;
    private String petName;
    private Integer petCurrentHp;
    private Integer petMaxHp;

    private String monsterName;
    private Integer monsterCurrentHp;
    private Integer monsterMaxHp;

    private List<String> logs;

    // 药剂数量
    private Integer hpPotionCount;
    private Integer ppPotionCount;

    // 技能PP数据（用于前端更新）
    private List<Map<String, Object>> skills;
}