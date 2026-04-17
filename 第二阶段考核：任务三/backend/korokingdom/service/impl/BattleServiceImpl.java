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
    @Transactional  //声明事务管理
    //战斗开始，先检测是否有未完成的战斗，然后再把战斗存入数据库，并返回战斗状态
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

        //断线重连功能
        //检测数据库是否有正在ACTIVE状态的战斗，如果有的话，则返回该战斗的当前状态
        LambdaQueryWrapper<Battle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Battle::getPetId, petId)
                .eq(Battle::getStatus, "ACTIVE");
        Battle existingBattle = this.baseMapper.selectOne(wrapper);
        if (existingBattle != null) {
            return getBattleState(existingBattle.getId());
        }

        List<BattleSkillState> skills = new ArrayList<>();
        List<Skill> petSkills = SkillData.getSkillsByType(pet.getPetType());        //根据属性把多个技能拿到petSkills
        for (Skill skill : petSkills) {                 //历遍petSkills，把技能一个个存进skill
            BattleSkillState state = new BattleSkillState();
            state.setSkill(skill);                      //再把skill放入state
            state.setCurrentPp(skill.getMaxPp());
            skills.add(state);
        }

        //初始化buff
        BattleBuff buff = new BattleBuff();
        buff.setAttackBoost(0.0);
        buff.setDefenseBoost(0.0);

        //初始化玩家技能数据
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
        //初始化怪物技能数据

        //读取背包药剂数量
        Integer hpPotionCount = pet.getHpPotionCount() != null ? pet.getHpPotionCount() : 0;
        Integer ppPotionCount = pet.getPpPotionCount() != null ? pet.getPpPotionCount() : 0;

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
        battleState.put("hpPotionCount", hpPotionCount);
        battleState.put("ppPotionCount", ppPotionCount);
        battleState.put("hpPotionUsed", false);
        battleState.put("ppPotionUsed", false);

        //把 battleState 存进数据库
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
        battle.setHpPotionUsed(false);
        battle.setPpPotionUsed(false);
        this.baseMapper.insert(battle);

        return buildBattleResp(battle, battleState);
    }

    @Override
    @Transactional
//战斗进行中，处理战斗动作
    public BattleResp executeAction(Long battleId, BattleActionReq req) {

        Battle battle = this.baseMapper.selectById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗不存在");
        }
        if (!"ACTIVE".equals(battle.getStatus())) {
            throw new BusinessException("战斗已结束");
        }

        //限制操作间隔，防止脚本快速刷请求
        if (battle.getLastActionTime() != null) {
            long interval = System.currentTimeMillis() - battle.getLastActionTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
            if (interval < MIN_ACTION_INTERVAL && interval > 0) {
                throw new BusinessException("操作过快，请稍后再试");
            }
        }

        //保证客户端发送的回合序列号是递增的
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

        //取出各种数据
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

        //投降
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

        // ==================== 第0.5层：使用药剂（不占用回合，不增加序列号） ====================
        // 获取药剂数量
        Integer hpPotionCount = (Integer) battleState.getOrDefault("hpPotionCount", 0);
        Integer ppPotionCount = (Integer) battleState.getOrDefault("ppPotionCount", 0);
        Boolean hpPotionUsed = (Boolean) battleState.getOrDefault("hpPotionUsed", false);
        Boolean ppPotionUsed = (Boolean) battleState.getOrDefault("ppPotionUsed", false);
        boolean usedPotion = false;

        // 使用HP药剂
        if (req.getUseHpPotion() != null && req.getUseHpPotion()) {
            if (hpPotionUsed) {
                logs.add("本场战斗已使用过HP药剂，不能再使用");
            } else if (hpPotionCount <= 0) {
                logs.add("背包中没有HP药剂");
            } else {
                // 扣减背包数量
                hpPotionCount--;
                battleState.put("hpPotionCount", hpPotionCount);
                battleState.put("hpPotionUsed", true);
                battle.setHpPotionUsed(true);
                usedPotion = true;

                // 回复40%血量
                int maxHp = pet.getHp();
                int healAmount = (int) (maxHp * 0.4);
                petCurrentHp = Math.min(petCurrentHp + healAmount, maxHp);
                logs.add(pet.getName() + "使用了HP药剂，回复了" + healAmount + "点生命值");

                // 更新数据库中的背包数量
                pet.setHpPotionCount(hpPotionCount);
                petService.updateById(pet);
            }
        }

        // 使用PP药剂
        if (req.getUsePpPotion() != null && req.getUsePpPotion()) {
            if (ppPotionUsed) {
                logs.add("本场战斗已使用过PP药剂，不能再使用");
            } else if (ppPotionCount <= 0) {
                logs.add("背包中没有PP药剂");
            } else {
                // 扣减背包数量
                ppPotionCount--;
                battleState.put("ppPotionCount", ppPotionCount);
                battleState.put("ppPotionUsed", true);
                battle.setPpPotionUsed(true);
                usedPotion = true;

                // 每个技能回复2点PP（跳过maxPp=-1的无限技能）
                for (Map<String, Object> skillData : skillsData) {
                    Integer maxPp = (Integer) skillData.get("maxPp");
                    if (maxPp != null && maxPp != -1) {
                        Integer currentPp = (Integer) skillData.get("currentPp");
                        int newPp = Math.min(currentPp + 2, maxPp);
                        skillData.put("currentPp", newPp);
                    }
                }
                logs.add(pet.getName() + "使用了PP药剂，所有技能PP回复2点");

                // 更新数据库中的背包数量
                pet.setPpPotionCount(ppPotionCount);
                petService.updateById(pet);
            }
        }

        // ==================== 第1层：玩家选择技能 ====================
        // 如果使用了药剂，跳过技能处理
        if (!usedPotion && "SKILL".equals(req.getActionType())) {
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

            // 第1.1层：攻击技能
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

                // 暂存玩家对怪物的伤害（不立即扣血，等第4层统一计算）
                // 这里先扣掉是为了保持原有逻辑，后续可重构为暂存
                monsterCurrentHp -= damage;
                logs.add(pet.getName() + "使用了" + skill.getName() + "，造成" + damage + "点伤害");

                if (random.nextInt(100) < 30) {
                    String taunt = aiService.generateTaunt(pet, level, petCurrentHp, monsterCurrentHp);
                    logs.add(level.getMonsterName() + "嘲讽道：" + taunt);
                }

                // 第1.2层：状态技能
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

                // 第1.3层：防御技能
            } else if ("DEFENSE".equals(skillType)) {
                battleState.put("activeDefenseSkillId", skill.getId());
                logs.add(pet.getName() + "使用了" + skill.getName() + "，进入防御状态");
            }

            battleState.put("skills", skillsData);
            battleState.put("buff", buffData);
        }

        // 第2层：检查怪物是否死亡（玩家攻击技能可能直接打死）
        if (monsterCurrentHp <= 0) {
            battle.setStatus("VICTORY");
            logs.add("战斗胜利！击败了" + level.getMonsterName());
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

        // 第3层：记录玩家防御技能效果（从上一回合存储的标记中取出）
        Integer activeDefenseSkillId = (Integer) battleState.get("activeDefenseSkillId");
        Skill activeDefense = null;
        double damageReduction = 0;
        double reflectRate = 0;
        double healRate = 0;
        if (activeDefenseSkillId != null) {
            activeDefense = SkillData.getSkillById(activeDefenseSkillId);
            battleState.remove("activeDefenseSkillId");
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
        }

        // 计算玩家最终防御力（考虑增益Buff）
        double defenseBoost = (Double) buffData.get("defenseBoost");
        int finalDefense = pet.getDefense();
        if (defenseBoost > 0) {
            finalDefense = (int) (pet.getDefense() * (1 + defenseBoost));
        }

        // ==================== 第4层：怪物选择技能 ====================
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

        // 第4.1层：怪物攻击技能
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

            // 第4.2层：怪物状态技能
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

            // 第4.3层：怪物防御技能
        } else if ("DEFENSE".equals(monsterSkillType)) {
            battleState.put("monsterActiveDefenseSkillId", monsterSkill.getId());
            logs.add(level.getMonsterName() + "使用了" + monsterSkill.getName() + "，进入防御状态");
        }

        // ==================== 第5层：记录怪物防御技能效果 ====================
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

        // ==================== 第6层：计算双方实际扣血量 ====================
        // 玩家对怪物的伤害已经在第1层扣过了，这里需要应用怪物的减伤率重新计算
        // 注意：原有逻辑中玩家伤害已经直接扣了，这里为了保持流程，不再重复计算
        // 实际应该把玩家伤害暂存，在第6层统一计算并扣血

        // 怪物对玩家的伤害，应用玩家减伤率
        int actualMonsterDamage = monsterDamage;
        if (damageReduction > 0) {
            actualMonsterDamage = (int) (monsterDamage * (1 - damageReduction));
            logs.add(level.getMonsterName() + "的攻击被减免，实际造成" + actualMonsterDamage + "点伤害");
        } else if (monsterDamage > 0) {
            logs.add(level.getMonsterName() + "对你造成了" + actualMonsterDamage + "点伤害");
            if (random.nextInt(100) < 30) {
                String taunt = aiService.generateTaunt(pet, level, petCurrentHp, monsterCurrentHp);
                logs.add(level.getMonsterName() + "嘲讽道：" + taunt);
            }
        }

        // ==================== 第7层：扣血（先怪物后玩家） ====================
        // 注意：玩家对怪物的伤害已经在第1层扣过了，这里只扣怪物对玩家的伤害
        petCurrentHp -= actualMonsterDamage;

        // ==================== 第8层：检查是否有人死亡 ====================
        // 先检查玩家是否死亡（因为怪物死亡已经在第2层检查过了）
        if (petCurrentHp <= 0) {
            battle.setStatus("DEFEAT");
            logs.add("战斗失败，你被击败了");
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

        // ==================== 第9层：回血和反伤 ====================
        // 第9.1层：玩家回血
        if (healRate > 0 && activeDefense != null) {
            int healAmount = (int) (pet.getHp() * healRate);
            petCurrentHp = Math.min(petCurrentHp + healAmount, pet.getHp());
            logs.add(activeDefense.getName() + "回复了" + healAmount + "点生命值");
        }

        // 第9.2层：怪物回血
        if (monsterHealRate > 0 && monsterActiveDefense != null && monsterDamage > 0) {
            int monsterHealAmount = (int) (level.getMonsterHp() * monsterHealRate);
            monsterCurrentHp = Math.min(monsterCurrentHp + monsterHealAmount, level.getMonsterHp());
            logs.add(monsterActiveDefense.getName() + "回复了" + monsterHealAmount + "点生命值");
        }

        // 第9.3层：玩家反伤
        if (reflectRate > 0 && activeDefense != null && monsterDamage > 0) {
            int reflectDamage = (int) (actualMonsterDamage * reflectRate);
            monsterCurrentHp -= reflectDamage;
            logs.add(activeDefense.getName() + "反弹了" + reflectDamage + "点伤害给" + level.getMonsterName());
        }

        // 第9.4层：反伤后再次检查怪物死亡
        if (monsterCurrentHp <= 0) {
            battle.setStatus("VICTORY");
            logs.add("战斗胜利！击败了" + level.getMonsterName());
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

        // ==================== 第10层：更新战斗状态 ====================
        battleState.put("petCurrentHp", petCurrentHp);
        battleState.put("monsterCurrentHp", monsterCurrentHp);
        battleState.put("roundSeq", req.getRoundSeq());
        battleState.put("logs", logs);
        battleState.put("monsterSkills", monsterSkillsData);

        // ==================== 第11层：保存到数据库 ====================
        battle.setCurrentRound(battle.getCurrentRound() + 1);
        battle.setLastActionTime(LocalDateTime.now());
        try {
            battle.setBattleState(objectMapper.writeValueAsString(battleState));
        } catch (JsonProcessingException e) {
            throw new BusinessException("保存战斗状态失败");
        }
        this.baseMapper.updateById(battle);

        // ==================== 第12层：保存战斗日志 ====================
        BattleLog battleLog = new BattleLog();
        battleLog.setBattleId(battleId);
        battleLog.setRoundNum(battle.getCurrentRound());
        battleLog.setActionSeq(req.getRoundSeq());
        if (!logs.isEmpty()) {
            battleLog.setMessage(logs.get(logs.size() - 1));
        }
        battleLogMapper.insert(battleLog);

        // ==================== 第13层：返回结果 ====================
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

    //先根据ID查询战斗记录，若不存在则抛异常。然后将数据库中存储的JSON字符串反序列化为Map对象，解析战斗状态快照。最后组装并返回完整的战斗响应数据，用于前端展示或断线重连
    @Override
    public BattleResp getBattleState(Long battleId) {

        Battle battle = this.baseMapper.selectById(battleId);
        if (battle == null) {
            throw new BusinessException("战斗不存在");
        }

        Map<String, Object> battleState;
        try {
            battleState = objectMapper.readValue(battle.getBattleState(), Map.class);  //以JSON字符串解析战斗状态
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

        // 返回药剂数量
        resp.setHpPotionCount((Integer) battleState.getOrDefault("hpPotionCount", 0));
        resp.setPpPotionCount((Integer) battleState.getOrDefault("ppPotionCount", 0));

        // 返回技能PP数据（用于前端更新）
        resp.setSkills((List<Map<String, Object>>) battleState.get("skills"));

        List<String> logs = (List<String>) battleState.get("logs");
        if (logs != null && logs.size() > 10) {
            resp.setLogs(logs.subList(logs.size() - 10, logs.size()));
        } else if (logs != null) {
            resp.setLogs(logs);
        }

        return resp;
    }
}