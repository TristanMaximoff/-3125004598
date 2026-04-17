package com.game.korokingdom.dto.response;

import lombok.Data;

@Data
public class PetResp {
    private Long id;
    private String name;
    private String petType;
    private Integer level;
    private Integer exp;
    private Integer hp;
    private Integer attack;
    private Integer defense;
    private Integer individualValue;
    private Integer currentHp;
    private Integer currentLevel;
}