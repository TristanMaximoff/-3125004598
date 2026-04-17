package com.game.korokingdom.dto.request;

import lombok.Data;

@Data
public class CreatePetReq {
    private String name;      // 精灵名称，本质上没作用，除非初始精灵一个系别对应多个精灵
    private String petType;   // 系别：GRASS/FIRE/WATER
}