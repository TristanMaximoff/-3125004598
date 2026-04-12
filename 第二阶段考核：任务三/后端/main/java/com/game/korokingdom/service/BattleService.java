package com.game.korokingdom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.game.korokingdom.entity.Battle;
import com.game.korokingdom.dto.request.BattleActionReq;
import com.game.korokingdom.dto.response.BattleResp;

public interface BattleService extends IService<Battle> {

    // 开始战斗
    BattleResp startBattle(Long petId, Integer levelId);

    // 执行行动
    BattleResp executeAction(Long battleId, BattleActionReq req);

    // 投降
    BattleResp surrender(Long battleId);

    // 获取战斗状态（断线重连用）
    BattleResp getBattleState(Long battleId);

    // 恢复战斗
    BattleResp resumeBattle(Long battleId);
}