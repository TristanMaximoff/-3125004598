package com.game.korokingdom.dto.response;

import lombok.Data;
import java.util.List;

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
}