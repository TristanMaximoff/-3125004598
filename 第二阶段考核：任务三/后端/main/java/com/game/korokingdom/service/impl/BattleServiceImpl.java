package com.game.korokingdom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.korokingdom.dto.BattleBuff;
import com.game.korokingdom.dto.BattleSkillState;
import com.game.korokingdom.dto.request.BattleActionReq;
import com.game.korokingdom.dto.response.BattleResp;
import com.game.korokingdom.entity.*;
import com.game.korokingdom.exception.BusinessException;
import com.game.korokingdom.mapper.BattleLogMapper;
import com.game.korokingdom.mapper.BattleMapper;
import com.game.korokingdom.service.BattleService;
import com.game.korokingdom.service.LevelService;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.utils.DamageCalculator;
import com.game.korokingdom.utils.SkillData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BattleServiceImpl extends ServiceImpl<BattleMapper, Battle> implements BattleService {

    private final PetService petService;
    private final LevelService levelService;
    private final DamageCalculator damageCalculator;
    private final BattleLogMapper battleLogMapper;
    private final ObjectMapper objectMapper;

    private static final long MIN_ACTION_INTERVAL = 300;

    @Override
    @Transactional
    public BattleResp startBattle(Long petId, Integer levelId) {
        Pet pet = petService.getById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }

        Level level = levelService.getById(levelId);
        if (level == null) {
            throw new BusinessException("关卡不存在");
        }

        if (level.getLevelNum() > pet.getCurrentLevel()) {
            throw new BusinessException("关卡未解锁");
        }

        LambdaQueryWrapper<Battle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Battle::getPetId, petId)
                .eq(Battle::getStatus, "ACTIVE");
        Battle existingBattle = this.baseMapper.selectOne(wrapper);
        if (existingBattle != null) {
            return getBattleState(existingBattle.getId());
        }

        List<BattleSkillState> skills = new ArrayList<>();
        List<Skill> petSkills = SkillData.getSkillsByType(pet.getPetType());
        for (Skill skill : petSkills) {
            BattleSkillState state = new BattleSkillState();
            state.setSkill(skill);
            state.setCurrentPp(skill.getMaxPp());
            skills.add(state);
        }

        BattleBuff buff = new BattleBuff();
        buff.setAttackBoost(0.0);
        buff.setDefenseBoost(0.0);

        List<Map<String, Object>> skillsData = new ArrayList<>();
        for (BattleSkillState state : skills) {
            Map<String, Object> skillMap = new HashMap<>();
            skillMap.put("id", state.getSkill().getId());
            skillMap.put("name", state.getSkill().getName());
            skillMap.put("skillType", state.getSkill().getSkillType());
            skillMap.put("maxPp", state.getSkill().getMaxPp());
            skillMap.put("currentPp", state.getCurrentPp());
            skillMap.put("buffTarget", state.getSkill().getBuffTarget());
            skillMap.put("buffValue", state.getSkill().getBuffValue());
            skillMap.put("damageReduction", state.getSkill().getDamageReduction());
            skillMap.put("reflectRate", state.getSkill().getReflectRate());
            skillMap.put("healRate", state.getSkill().getHealRate());
            skillsData.add(skillMap);
        }

        Map<String, Object> battleState = new HashMap<>();
        battleState.put("pet", pet);
        battleState.put("level", level);
        battleState.put("petCurrentHp", pet.getCurrentHp());
        battleState.put("monsterCurrentHp", level.getMonsterHp());
        battleState.put("skills", skillsData);
        battleState.put("buff", buff);
        battleState.put("roundSeq", 0);
        battleState.put("logs", new ArrayList<String>());

        String battleStateJson;
        try {
            battleStateJson = objectMapper.writeValueAsString(battleState);
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存战斗状态失败");
        }

        Battle battle = new Battle();
        battle.setPetId(petId);
        battle.setLevelId(levelId);
        battle.setBattleState(battleStateJson);
        battle.setCurrentRound(0);
        battle.setLastActionTime(LocalDateTime.now());
        battle.setStatus("ACTIVE");
        this.baseMapper.insert(battle);

        return buildBattleResp(battle, battleState);
    }

    @Override
    @Transactional
    public BattleResp executeAction(Long battleId, BattleActionReq req) {
        Battle battle = this.baseMapper.selectById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗不存在");
        }
        if (!"ACTIVE".equals(battle.getStatus())) {
            throw new BusinessException("战斗已结束");
        }

        if (battle.getLastActionTime() != null) {
            long interval = System.currentTimeMillis() - battle.getLastActionTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
            if (interval < MIN_ACTION_INTERVAL && interval > 0) {
                throw new BusinessException("操作过快，请稍后再试");
            }
        }

        Map<String, Object> battleState;
        try {
            battleState = objectMapper.readValue(battle.getBattleState(), Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("解析战斗状态失败");
        }

        Integer currentSeq = (Integer) battleState.get("roundSeq");
        if (req.getRoundSeq() != null && currentSeq != null && req.getRoundSeq() <= currentSeq) {
            throw new BusinessException("无效的回合序列号");
        }

        Map<String, Object> petMap = (Map<String, Object>) battleState.get("pet");
        Integer petCurrentHp = (Integer) battleState.get("petCurrentHp");
        Integer monsterCurrentHp = (Integer) battleState.get("monsterCurrentHp");
        List<String> logs = (List<String>) battleState.get("logs");
        List<Map<String, Object>> skillsData = (List<Map<String, Object>>) battleState.get("skills");
        Map<String, Object> buffData = (Map<String, Object>) battleState.get("buff");

        if (logs == null) {
            logs = new ArrayList<>();
        }

        Pet pet = petService.getById(battle.getPetId());
        Level level = levelService.getById(battle.getLevelId());

        if ("SURRENDER".equals(req.getActionType())) {
            battle.setStatus("SURRENDER");
            logs.add("你投降了，战斗失败");
            battleState.put("logs", logs);
            try {
                battle.setBattleState(objectMapper.writeValueAsString(battleState));
            } catch (JsonProcessingException e) {
                // ignore
            }
            this.baseMapper.updateById(battle);
            return buildBattleResp(battle, battleState);
        }

        if ("SKILL".equals(req.getActionType())) {
            int skillId = req.getSkillId();
            Skill skill = SkillData.getSkillById(skillId);
            if (skill == null) {
                throw new BusinessException("技能不存在");
            }

            Map<String, Object> targetSkillData = null;
            for (Map<String, Object> skillData : skillsData) {
                if (((Integer) skillData.get("id")) == skillId) {
                    targetSkillData = skillData;
                    break;
                }
            }

            if (targetSkillData == null) {
                throw new BusinessException("精灵没有该技能");
            }

            int currentPp = (Integer) targetSkillData.get("currentPp");
            if (currentPp <= 0 && ((Integer) targetSkillData.get("maxPp")) != -1) {
                throw new BusinessException("技能使用次数已用完");
            }

            if (((Integer) targetSkillData.get("maxPp")) != -1) {
                targetSkillData.put("currentPp", currentPp - 1);
            }

            String skillType = (String) targetSkillData.get("skillType");

            if ("ATTACK".equals(skillType)) {
                double attackBoost = (Double) buffData.get("attackBoost");
                int finalAttack = pet.getAttack();
                if (attackBoost > 0) {
                    finalAttack = (int) (pet.getAttack() * (1 + attackBoost));
                }

                int damage = damageCalculator.calculateDamage(
                        finalAttack, level.getMonsterDefense(),
                        pet.getPetType(), level.getMonsterType(),
                        pet.getLevel(), 1
                );

                monsterCurrentHp -= damage;
                logs.add(pet.getName() + "使用了" + skill.getName() + "，造成" + damage + "点伤害");

            } else if ("STATUS".equals(skillType)) {
                Integer buffTarget = (Integer) targetSkillData.get("buffTarget");
                Double buffValue = (Double) targetSkillData.get("buffValue");

                if (buffTarget == 1) {
                    double currentBoost = (Double) buffData.get("attackBoost");
                    double newBoost = currentBoost + buffValue;
                    buffData.put("attackBoost", newBoost);
                    logs.add(pet.getName() + "使用了" + skill.getName() + "，攻击力提升" + (int)(buffValue * 100) + "%");
                } else if (buffTarget == 2) {
                    double currentBoost = (Double) buffData.get("defenseBoost");
                    double newBoost = currentBoost + buffValue;
                    buffData.put("defenseBoost", newBoost);
                    logs.add(pet.getName() + "使用了" + skill.getName() + "，防御力提升" + (int)(buffValue * 100) + "%");
                }

            } else if ("DEFENSE".equals(skillType)) {
                battleState.put("activeDefenseSkillId", skill.getId());
                logs.add(pet.getName() + "使用了" + skill.getName() + "，进入防御状态");
            }

            battleState.put("skills", skillsData);
            battleState.put("buff", buffData);
        }

        if (monsterCurrentHp <= 0) {
            battle.setStatus("VICTORY");
            logs.add("战斗胜利！击败了" + level.getMonsterName());
            battleState.put("monsterCurrentHp", 0);
            battleState.put("logs", logs);
            try {
                battle.setBattleState(objectMapper.writeValueAsString(battleState));
            } catch (JsonProcessingException e) {
                // ignore
            }
            battle.setUpdatedAt(LocalDateTime.now());
            this.baseMapper.updateById(battle);
            return buildBattleResp(battle, battleState);
        }

        Integer activeDefenseSkillId = (Integer) battleState.get("activeDefenseSkillId");
        Skill activeDefense = null;
        if (activeDefenseSkillId != null) {
            activeDefense = SkillData.getSkillById(activeDefenseSkillId);
            battleState.remove("activeDefenseSkillId");
        }

        double defenseBoost = (Double) buffData.get("defenseBoost");
        int finalDefense = pet.getDefense();
        if (defenseBoost > 0) {
            finalDefense = (int) (pet.getDefense() * (1 + defenseBoost));
        }

        int monsterDamage = damageCalculator.calculateDamage(
                level.getMonsterAttack(), finalDefense,
                level.getMonsterType(), pet.getPetType(),
                1, pet.getLevel()
        );

        double damageReduction = 0;
        double reflectRate = 0;
        double healRate = 0;

        if (activeDefense != null) {
            if (activeDefense.getDamageReduction() != null) {
                damageReduction = activeDefense.getDamageReduction();
            }
            if (activeDefense.getReflectRate() != null) {
                reflectRate = activeDefense.getReflectRate();
            }
            if (activeDefense.getHealRate() != null) {
                healRate = activeDefense.getHealRate();
            }
        }

        int actualDamage = monsterDamage;
        if (damageReduction > 0) {
            actualDamage = (int) (monsterDamage * (1 - damageReduction));
            logs.add(level.getMonsterName() + "的攻击被减免，实际造成" + actualDamage + "点伤害");
        } else {
            logs.add(level.getMonsterName() + "对你造成了" + actualDamage + "点伤害");
        }

        petCurrentHp -= actualDamage;

        if (reflectRate > 0 && activeDefense != null) {
            int reflectDamage = (int) (actualDamage * reflectRate);
            monsterCurrentHp -= reflectDamage;
            logs.add(activeDefense.getName() + "反弹了" + reflectDamage + "点伤害给" + level.getMonsterName());
        }

        if (healRate > 0 && activeDefense != null) {
            int healAmount = (int) (pet.getHp() * healRate);
            petCurrentHp = Math.min(petCurrentHp + healAmount, pet.getHp());
            logs.add(activeDefense.getName() + "回复了" + healAmount + "点生命值");
        }

        if (petCurrentHp <= 0) {
            battle.setStatus("DEFEAT");
            logs.add("战斗失败，你被击败了");
            battleState.put("petCurrentHp", 0);
            battleState.put("logs", logs);
            try {
                battle.setBattleState(objectMapper.writeValueAsString(battleState));
            } catch (JsonProcessingException e) {
                // ignore
            }
            battle.setUpdatedAt(LocalDateTime.now());
            this.baseMapper.updateById(battle);
            return buildBattleResp(battle, battleState);
        }

        battleState.put("petCurrentHp", petCurrentHp);
        battleState.put("monsterCurrentHp", monsterCurrentHp);
        battleState.put("roundSeq", req.getRoundSeq());
        battleState.put("logs", logs);

        battle.setCurrentRound(battle.getCurrentRound() + 1);
        battle.setLastActionTime(LocalDateTime.now());
        try {
            battle.setBattleState(objectMapper.writeValueAsString(battleState));
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存战斗状态失败");
        }
        this.baseMapper.updateById(battle);

        BattleLog battleLog = new BattleLog();
        battleLog.setBattleId(battleId);
        battleLog.setRoundNum(battle.getCurrentRound());
        battleLog.setActionSeq(req.getRoundSeq());
        battleLog.setMessage(logs.get(logs.size() - 1));
        battleLogMapper.insert(battleLog);

        return buildBattleResp(battle, battleState);
    }

    @Override
    @Transactional
    public BattleResp surrender(Long battleId) {
        Battle battle = this.baseMapper.selectById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗不存在");
        }
        if (!"ACTIVE".equals(battle.getStatus())) {
            throw new BusinessException("战斗已结束");
        }

        battle.setStatus("SURRENDER");
        battle.setUpdatedAt(LocalDateTime.now());
        this.baseMapper.updateById(battle);

        Map<String, Object> battleState;
        try {
            battleState = objectMapper.readValue(battle.getBattleState(), Map.class);
        } catch (JsonProcessingException e) {
            battleState = new HashMap<>();
        }
        List<String> logs = (List<String>) battleState.get("logs");
        if (logs == null) {
            logs = new ArrayList<>();
        }
        logs.add("你投降了，战斗失败");
        battleState.put("logs", logs);
        try {
            battle.setBattleState(objectMapper.writeValueAsString(battleState));
        } catch (JsonProcessingException e) {
            // ignore
        }
        this.baseMapper.updateById(battle);

        return buildBattleResp(battle, battleState);
    }

    @Override
    public BattleResp getBattleState(Long battleId) {
        Battle battle = this.baseMapper.selectById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗不存在");
        }
        Map<String, Object> battleState;
        try {
            battleState = objectMapper.readValue(battle.getBattleState(), Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("解析战斗状态失败");
        }
        return buildBattleResp(battle, battleState);
    }

    @Override
    public BattleResp resumeBattle(Long battleId) {
        return getBattleState(battleId);
    }

    private BattleResp buildBattleResp(Battle battle, Map<String, Object> battleState) {
        BattleResp resp = new BattleResp();
        resp.setBattleId(battle.getId());
        resp.setStatus(battle.getStatus());
        resp.setCurrentRound(battle.getCurrentRound());

        Object petObj = battleState.get("pet");
        if (petObj instanceof Map) {
            Map<String, Object> petMap = (Map<String, Object>) petObj;
            resp.setPetId(Long.valueOf(petMap.get("id").toString()));
            resp.setPetName((String) petMap.get("name"));
            resp.setPetMaxHp((Integer) petMap.get("hp"));
        } else if (petObj instanceof Pet) {
            Pet pet = (Pet) petObj;
            resp.setPetId(pet.getId());
            resp.setPetName(pet.getName());
            resp.setPetMaxHp(pet.getHp());
        }

        resp.setPetCurrentHp((Integer) battleState.get("petCurrentHp"));

        Object levelObj = battleState.get("level");
        if (levelObj instanceof Map) {
            Map<String, Object> levelMap = (Map<String, Object>) levelObj;
            resp.setMonsterName((String) levelMap.get("monsterName"));
            resp.setMonsterMaxHp((Integer) levelMap.get("monsterHp"));
        } else if (levelObj instanceof Level) {
            Level level = (Level) levelObj;
            resp.setMonsterName(level.getMonsterName());
            resp.setMonsterMaxHp(level.getMonsterHp());
        }
        resp.setMonsterCurrentHp((Integer) battleState.get("monsterCurrentHp"));

        List<String> logs = (List<String>) battleState.get("logs");
        if (logs != null && logs.size() > 10) {
            resp.setLogs(logs.subList(logs.size() - 10, logs.size()));
        } else if (logs != null) {
            resp.setLogs(logs);
        }

        return resp;
    }
    }