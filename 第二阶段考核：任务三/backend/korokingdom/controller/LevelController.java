package com.game.korokingdom.controller;

import com.game.korokingdom.annotation.RequireLogin;
import com.game.korokingdom.dto.response.LevelResp;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.exception.BusinessException;
import com.game.korokingdom.service.BattleService;
import com.game.korokingdom.service.LevelService;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.game.korokingdom.entity.Battle;
import com.game.korokingdom.entity.Pet;

@RestController
@RequestMapping("/level")
@RequiredArgsConstructor
public class LevelController {

    private final LevelService levelService;
    private final PetService petService;
    private final JwtUtil jwtUtil;
    private final BattleService battleService;

    // 获取所有关卡列表
    @GetMapping("/list")
    public Map<String, Object> getAllLevels() {
        List<LevelResp> levels = levelService.getAllLevels();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", levels);
        return result;
    }

    // 获取当前关卡
    @GetMapping("/current")
    @RequireLogin
    public Map<String, Object> getCurrentLevel(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        LevelResp level = levelService.getCurrentLevel(petId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", level);
        return result;
    }

    // 获取下一关信息
    @GetMapping("/next")
    @RequireLogin
    public Map<String, Object> getNextLevel(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        LevelResp level = levelService.getNextLevel(petId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", level);
        return result;
    }

    // 战斗胜利后领取奖励
    @PostMapping("/claim-reward")
    public Map<String, Object> claimReward(@RequestHeader("Authorization") String authorization,
                                           @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        Pet pet = petService.getById(petId);

        // 1. 获取战斗记录
        Battle battle = battleService.getById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗记录不存在");
        }
        if (!"VICTORY".equals(battle.getStatus())) {
            throw new BusinessException("战斗未胜利，无法领取奖励");
        }

        // 2. 检查是否已领取过奖励
        if (battle.getRewardClaimed() != null && battle.getRewardClaimed()) {
            throw new BusinessException("奖励已领取");
        }

        // 3. 获取关卡信息
        Level level = levelService.getById(battle.getLevelId());

        // 4. 计算经验奖励
        int expReward = level.getExpReward();
        boolean isFirstClear = (pet.getCurrentLevel() == level.getLevelNum());
        if (isFirstClear) {
            expReward += level.getFirstExpBonus();
        }

        // 5. 发放经验
        petService.addExp(petId, expReward);

        // 6. 如果是首次通关，解锁下一关（最后一关不报错）
        if (isFirstClear) {
            try {
                levelService.unlockNextLevel(petId);
            } catch (BusinessException e) {
                // 已经是最后一关，忽略异常，不影响领取奖励
            }
        }

        // 6.5 随机发放药剂（重新查询最新数据，避免覆盖经验和解锁的更新）
        Random random = new Random();
        String potionType = null;
// 重新从数据库查询最新的精灵对象
        Pet latestPet = petService.getById(petId);
        if (random.nextBoolean()) {
            int newCount = (latestPet.getHpPotionCount() == null ? 0 : latestPet.getHpPotionCount()) + 1;
            latestPet.setHpPotionCount(newCount);
            potionType = "HP药剂";
        } else {
            int newCount = (latestPet.getPpPotionCount() == null ? 0 : latestPet.getPpPotionCount()) + 1;
            latestPet.setPpPotionCount(newCount);
            potionType = "PP药剂";
        }
        petService.updateById(latestPet);

        // 7. 标记奖励已领取
        battle.setRewardClaimed(true);
        battleService.updateById(battle);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "奖励领取成功");
        result.put("expGained", expReward);
        result.put("isFirstClear", isFirstClear);
        result.put("potionReward", potionType);
        return result;
    }

}