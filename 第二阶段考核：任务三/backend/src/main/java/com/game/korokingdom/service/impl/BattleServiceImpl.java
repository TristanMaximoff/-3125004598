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
import com.game.korokingdom.service.AiService;
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
import java.util.Random;

@Service
@RequiredArgsConstructor
public class BattleServiceImpl extends ServiceImpl<BattleMapper, Battle> implements BattleService {

    private final PetService petService;
    private final LevelService levelService;
    private final DamageCalculator damageCalculator;
    private final BattleLogMapper battleLogMapper;
    private final ObjectMapper objectMapper;
    private final AiService aiService;

    private static final long MIN_ACTION_INTERVAL = 300;
    private final Random random = new Random();

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

        List<Map<String, Object>> monsterSkillsData = new ArrayList<>();
        List<Skill> monsterSkills = SkillData.getSkillsByMonsterType(level.getMonsterType());
        for (Skill skill : monsterSkills) {
            Map<String, Object> skillMap = new HashMap<>();
            skillMap.put("id", skill.getId());
            skillMap.put("name", skill.getName());
            skillMap.put("skillType", skill.getSkillType());
            skillMap.put("maxPp", skill.getMaxPp());
            skillMap.put("currentPp", skill.getMaxPp());
            skillMap.put("buffTarget", skill.getBuffTarget());
            skillMap.put("buffValue", skill.getBuffValue());
            skillMap.put("damageReduction", skill.getDamageReduction());
            skillMap.put("reflectRate", skill.getReflectRate());
            skillMap.put("healRate", skill.getHealRate());
            monsterSkillsData.add(skillMap);
        }

        Map<String, Object> battleState = new HashMap<>();
        battleState.put("pet", pet);
        battleState.put("level", level);
        battleState.put("petCurrentHp", pet.getCurrentHp());
        battleState.put("monsterCurrentHp", level.getMonsterHp());
        battleState.put("skills", skillsData);
        battleState.put("monsterSkills", monsterSkillsData);
        battleState.put("buff", buff);
        battleState.put("monsterBuff", new HashMap<String, Double>());
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

        Integer petCurrentHp = (Integer) battleState.get("petCurrentHp");
        Integer monsterCurrentHp = (Integer) battleState.get("monsterCurrentHp");
        List<String> logs = (List<String>) battleState.get("logs");
        List<Map<String, Object>> skillsData = (List<Map<String, Object>>) battleState.get("skills");
        List<Map<String, Object>> monsterSkillsData = (List<Map<String, Object>>) battleState.get("monsterSkills");
        Map<String, Object> buffData = (Map<String, Object>) battleState.get("buff");
        Map<String, Object> monsterBuffData = (Map<String, Object>) battleState.get("monsterBuff");

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

                // 玩家造成伤害后，30%概率触发怪物嘲讽
                if (random.nextInt(100) < 30) {
                    String taunt = aiService.generateTaunt(pet, level, petCurrentHp, monsterCurrentHp);
                    logs.add(level.getMonsterName() + "嘲讽道：" + taunt);
                }

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
            // 战斗胜利后生成AI战报总结
            String summary = aiService.generateBattleSummary(battle, pet, level);
            logs.add("【战报】" + summary);
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

        int monsterSkillId = decideMonsterSkill(level, monsterCurrentHp, level.getMonsterHp());
        Skill monsterSkill = SkillData.getSkillById(monsterSkillId);
        if (monsterSkill == null) {
            throw new BusinessException("怪物技能不存在");
        }

        Map<String, Object> targetMonsterSkillData = null;
        for (Map<String, Object> skillData : monsterSkillsData) {
            if (((Integer) skillData.get("id")) == monsterSkillId) {
                targetMonsterSkillData = skillData;
                break;
            }
        }

        if (targetMonsterSkillData != null) {
            int currentPp = (Integer) targetMonsterSkillData.get("currentPp");
            if (currentPp <= 0 && ((Integer) targetMonsterSkillData.get("maxPp")) != -1) {
                monsterSkill = SkillData.getSkillById(getDefaultAttackSkillId(level.getMonsterType()));
                monsterSkillId = monsterSkill.getId();
                for (Map<String, Object> skillData : monsterSkillsData) {
                    if (((Integer) skillData.get("id")) == monsterSkillId) {
                        targetMonsterSkillData = skillData;
                        break;
                    }
                }
            }
            if (targetMonsterSkillData != null && ((Integer) targetMonsterSkillData.get("maxPp")) != -1) {
                targetMonsterSkillData.put("currentPp", currentPp - 1);
            }
        }

        String monsterSkillType = monsterSkill.getSkillType();
        int monsterDamage = 0;
        double damageReduction = 0;
        double reflectRate = 0;
        double healRate = 0;

        if ("ATTACK".equals(monsterSkillType)) {
            double monsterAttackBoostValue = (Double) monsterBuffData.getOrDefault("monsterAttackBoost", 0.0);
            int finalMonsterAttack = level.getMonsterAttack();
            if (monsterAttackBoostValue > 0) {
                finalMonsterAttack = (int) (level.getMonsterAttack() * (1 + monsterAttackBoostValue));
            }

            monsterDamage = damageCalculator.calculateDamage(
                    finalMonsterAttack, finalDefense,
                    level.getMonsterType(), pet.getPetType(),
                    1, pet.getLevel()
            );
            logs.add(level.getMonsterName() + "使用了" + monsterSkill.getName());

        } else if ("STATUS".equals(monsterSkillType)) {
            Double buffValue = monsterSkill.getBuffValue();
            if (buffValue == null) {
                buffValue = 0.5;
            }
            double currentBoost = (Double) monsterBuffData.getOrDefault("monsterAttackBoost", 0.0);
            double newBoost = currentBoost + buffValue;
            monsterBuffData.put("monsterAttackBoost", newBoost);
            logs.add(level.getMonsterName() + "使用了" + monsterSkill.getName() + "，攻击力提升" + (int)(buffValue * 100) + "%");
            battleState.put("monsterBuff", monsterBuffData);

        } else if ("DEFENSE".equals(monsterSkillType)) {
            battleState.put("monsterActiveDefenseSkillId", monsterSkill.getId());
            logs.add(level.getMonsterName() + "使用了" + monsterSkill.getName() + "，进入防御状态");
        }

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

        Integer monsterActiveDefenseId = (Integer) battleState.get("monsterActiveDefenseSkillId");
        Skill monsterActiveDefense = null;
        double monsterDamageReduction = 0;
        double monsterHealRate = 0;
        if (monsterActiveDefenseId != null) {
            monsterActiveDefense = SkillData.getSkillById(monsterActiveDefenseId);
            if (monsterActiveDefense != null) {
                if (monsterActiveDefense.getDamageReduction() != null) {
                    monsterDamageReduction = monsterActiveDefense.getDamageReduction();
                }
                if (monsterActiveDefense.getHealRate() != null) {
                    monsterHealRate = monsterActiveDefense.getHealRate();
                }
            }
            battleState.remove("monsterActiveDefenseSkillId");
        }

        int actualDamage = monsterDamage;
        if (damageReduction > 0) {
            actualDamage = (int) (monsterDamage * (1 - damageReduction));
            logs.add(level.getMonsterName() + "的攻击被减免，实际造成" + actualDamage + "点伤害");
        } else if (monsterDamage > 0) {
            logs.add(level.getMonsterName() + "对你造成了" + actualDamage + "点伤害");

            // 怪物造成伤害后，30%概率触发怪物嘲讽
            if (random.nextInt(100) < 30) {
                String taunt = aiService.generateTaunt(pet, level, petCurrentHp, monsterCurrentHp);
                logs.add(level.getMonsterName() + "嘲讽道：" + taunt);
            }
        }

        petCurrentHp -= actualDamage;

        if (reflectRate > 0 && activeDefense != null && monsterDamage > 0) {
            int reflectDamage = (int) (actualDamage * reflectRate);
            monsterCurrentHp -= reflectDamage;
            logs.add(activeDefense.getName() + "反弹了" + reflectDamage + "点伤害给" + level.getMonsterName());
        }

        if (healRate > 0 && activeDefense != null) {
            int healAmount = (int) (pet.getHp() * healRate);
            petCurrentHp = Math.min(petCurrentHp + healAmount, pet.getHp());
            logs.add(activeDefense.getName() + "回复了" + healAmount + "点生命值");
        }

        if (monsterHealRate > 0 && monsterActiveDefense != null && monsterDamage > 0) {
            int monsterHealAmount = (int) (level.getMonsterHp() * monsterHealRate);
            monsterCurrentHp = Math.min(monsterCurrentHp + monsterHealAmount, level.getMonsterHp());
            logs.add(monsterActiveDefense.getName() + "回复了" + monsterHealAmount + "点生命值");
        }

        if (petCurrentHp <= 0) {
            battle.setStatus("DEFEAT");
            logs.add("战斗失败，你被击败了");
            // 战斗失败后生成AI战报总结
            String summary = aiService.generateBattleSummary(battle, pet, level);
            logs.add("【战报】" + summary);
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
        battleState.put("monsterSkills", monsterSkillsData);

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
        if (!logs.isEmpty()) {
            battleLog.setMessage(logs.get(logs.size() - 1));
        }
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

    private int decideMonsterSkill(Level level, int currentHp, int maxHp) {
        String monsterType = level.getMonsterType();
        int r = random.nextInt(100);
        double hpPercent = (double) currentHp / maxHp;

        if ("WATER".equals(monsterType)) {
            if (r < 50) {
                return 10;
            } else {
                return 11;
            }
        } else if ("GRASS".equals(monsterType)) {
            if (hpPercent < 0.3) {
                if (r < 80) {
                    return 9;
                } else {
                    return 7;
                }
            } else {
                if (r < 70) {
                    return 7;
                } else {
                    return 9;
                }
            }
        } else if ("FIRE".equals(monsterType)) {
            return 4;
        }

        return getDefaultAttackSkillId(monsterType);
    }

    private int getDefaultAttackSkillId(String monsterType) {
        switch (monsterType) {
            case "WATER":
                return 10;
            case "GRASS":
                return 7;
            case "FIRE":
                return 4;
            default:
                return 4;
        }
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