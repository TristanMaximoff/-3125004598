package com.game.korokingdom.service.impl;

import com.game.korokingdom.config.AiConfig;
import com.game.korokingdom.dto.request.AiChatReq;
import com.game.korokingdom.dto.response.AiChatResp;
import com.game.korokingdom.entity.Battle;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.entity.Pet;
import com.game.korokingdom.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final AiConfig aiConfig;

    //接收一个prompt作为ai提示词
    private String callAi(String prompt) {
        // TODO: 等拿到 API Key 后实现
        // 目前返回模拟数据
        return "【AI模拟】" + prompt.substring(0, Math.min(50, prompt.length())) + "...";
    }

    @Override
    public String generateTaunt(Pet pet, Level level, int petCurrentHp, int monsterCurrentHp) {
        String prompt = String.format(
                "怪物【%s】对玩家【%s】说一句嘲讽的话，20字以内。",
                level.getMonsterName(), pet.getName());
        return callAi(prompt);
    }

    @Override
    public String generateBattleSummary(Battle battle, Pet pet, Level level) {
        String result = "VICTORY".equals(battle.getStatus()) ? "胜利" : "失败";
        String prompt = String.format(
                "总结一场战斗，玩家%s系精灵【%s】%s了%s系怪物【%s】，30字以内。",
                pet.getPetType(), pet.getName(), result, level.getMonsterType(), level.getMonsterName());
        return callAi(prompt);
    }

    @Override
    public String generateStrategy(Pet pet, Level level) {
        String prompt = String.format(
                "玩家%s系精灵【%s】要打%s系怪物【%s】，给一句策略建议，20字以内。",
                pet.getPetType(), pet.getName(), level.getMonsterType(), level.getMonsterName());
        return callAi(prompt);
    }
}