package com.game.korokingdom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.game.korokingdom.entity.Pet;
import com.game.korokingdom.dto.request.CreatePetReq;
import com.game.korokingdom.dto.response.PetResp;

public interface PetService extends IService<Pet> {

    // 创建精灵
    PetResp createPet(Long userId, CreatePetReq req);

    // 获取精灵信息
    PetResp getPetInfo(Long petId);

    // 获取当前用户的精灵
    PetResp getMyPet(Long userId);

    // 增加经验
    void addExp(Long petId, int exp);

    // 升级检查
    void checkLevelUp(Long petId);
}