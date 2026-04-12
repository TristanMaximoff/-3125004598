package com.game.korokingdom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.dto.response.LevelResp;

import java.util.List;

public interface LevelService extends IService<Level> {

    // 获取所有关卡列表
    List<LevelResp> getAllLevels();

    // 获取当前关卡（根据精灵的currentLevel）
    LevelResp getCurrentLevel(Long petId);

    // 获取下一关信息
    LevelResp getNextLevel(Long petId);

    // 解锁下一关
    void unlockNextLevel(Long petId);
}