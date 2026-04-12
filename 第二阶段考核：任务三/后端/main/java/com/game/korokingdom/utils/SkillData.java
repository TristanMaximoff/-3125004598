package com.game.korokingdom.utils;

import com.game.korokingdom.entity.Skill;
import java.util.*;

public class SkillData {

    // 技能库
    public static final Map<Integer, Skill> SKILL_MAP = new HashMap<>();

    static {
        // ========== 水蓝蓝技能 ==========
        Skill skill1 = new Skill();
        skill1.setId(1);
        skill1.setName("水炮");
        skill1.setSkillType("ATTACK");
        skill1.setPower(0);
        skill1.setMaxPp(-1);  // 无限使用
        skill1.setDescription("发射水炮攻击敌人");

        Skill skill2 = new Skill();
        skill2.setId(2);
        skill2.setName("冥想");
        skill2.setSkillType("STATUS");
        skill2.setMaxPp(3);
        skill2.setBuffTarget(1);  // 攻击提升
        skill2.setBuffValue(1.0); // 提升100%
        skill2.setDescription("冥想使自身攻击提升100%，可叠加");

        Skill skill3 = new Skill();
        skill3.setId(3);
        skill3.setName("水幕");
        skill3.setSkillType("DEFENSE");
        skill3.setMaxPp(3);
        skill3.setDamageReduction(0.9); // 减少90%伤害
        skill3.setDescription("制造水幕，受到的伤害减少90%");

        // ========== 火花技能 ==========
        Skill skill4 = new Skill();
        skill4.setId(4);
        skill4.setName("火花");
        skill4.setSkillType("ATTACK");
        skill4.setMaxPp(-1);
        skill4.setDescription("向敌人喷射火花");

        Skill skill5 = new Skill();
        skill5.setId(5);
        skill5.setName("斗志");
        skill5.setSkillType("STATUS");
        skill5.setMaxPp(3);
        skill5.setBuffTarget(1);  // 攻击提升
        skill5.setBuffValue(1.0); // 提升100%
        skill5.setDescription("斗志昂扬，自身攻击提升100%，可叠加");

        Skill skill6 = new Skill();
        skill6.setId(6);
        skill6.setName("火焰护盾");
        skill6.setSkillType("DEFENSE");
        skill6.setMaxPp(3);
        skill6.setDamageReduction(0.5); // 减少50%伤害
        skill6.setReflectRate(0.5);     // 反伤50%
        skill6.setDescription("制造火焰护盾，受到的伤害减少50%，并反弹50%伤害");

        // ========== 喵喵技能 ==========
        Skill skill7 = new Skill();
        skill7.setId(7);
        skill7.setName("藤鞭");
        skill7.setSkillType("ATTACK");
        skill7.setMaxPp(-1);
        skill7.setDescription("用藤蔓抽打敌人");

        Skill skill8 = new Skill();
        skill8.setId(8);
        skill8.setName("硬化");
        skill8.setSkillType("STATUS");
        skill8.setMaxPp(3);
        skill8.setBuffTarget(2);  // 防御提升
        skill8.setBuffValue(0.5); // 提升50%
        skill8.setDescription("硬化皮肤，自身防御提升50%，可叠加");

        Skill skill9 = new Skill();
        skill9.setId(9);
        skill9.setName("光合作用");
        skill9.setSkillType("DEFENSE");
        skill9.setMaxPp(3);
        skill9.setDamageReduction(0.9); // 减少90%伤害
        skill9.setHealRate(0.3);        // 回复30%生命
        skill9.setDescription("光合作用，受到的伤害减少90%，并回复30%最大生命值");

        SKILL_MAP.put(1, skill1);
        SKILL_MAP.put(2, skill2);
        SKILL_MAP.put(3, skill3);
        SKILL_MAP.put(4, skill4);
        SKILL_MAP.put(5, skill5);
        SKILL_MAP.put(6, skill6);
        SKILL_MAP.put(7, skill7);
        SKILL_MAP.put(8, skill8);
        SKILL_MAP.put(9, skill9);
    }

    // 根据系别获取精灵可学习的技能
    public static List<Skill> getSkillsByType(String petType) {
        List<Skill> skills = new ArrayList<>();
        switch (petType) {
            case "WATER":
                skills.add(SKILL_MAP.get(1));
                skills.add(SKILL_MAP.get(2));
                skills.add(SKILL_MAP.get(3));
                break;
            case "FIRE":
                skills.add(SKILL_MAP.get(4));
                skills.add(SKILL_MAP.get(5));
                skills.add(SKILL_MAP.get(6));
                break;
            case "GRASS":
                skills.add(SKILL_MAP.get(7));
                skills.add(SKILL_MAP.get(8));
                skills.add(SKILL_MAP.get(9));
                break;
        }
        return skills;
    }

    // 根据ID获取技能
    public static Skill getSkillById(int id) {
        return SKILL_MAP.get(id);
    }
}