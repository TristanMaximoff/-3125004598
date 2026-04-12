package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class CreatePetReq {
    private String name;      // 精灵名称
    private String petType;   // 系别：GRASS/FIRE/WATER
}