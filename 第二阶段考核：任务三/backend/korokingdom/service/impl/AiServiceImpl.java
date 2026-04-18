package com.game.korokingdom.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.korokingdom.config.AiConfig;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    //接收一个prompt作为ai提示词
    private String callAi(String prompt) {
        String apiUrl = aiConfig.getApiUrl() + "/chat/completions";

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class);

            Map responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map> choices = (List<Map>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map message = (Map) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    return content.trim();
                }
            }
            return "AI回复解析失败";
        } catch (Exception e) {
            e.printStackTrace();
            return "AI调用失败：" + e.getMessage();
        }
    }

    @Override
    public String generateTaunt(Pet pet, Level level, int petCurrentHp, int monsterCurrentHp) {
        int petMaxHp = pet.getHp();
        int monsterMaxHp = level.getMonsterHp();
        int monsterDamageTaken = monsterMaxHp - monsterCurrentHp;  // 怪物已被扣的血

        String prompt = String.format(
                "怪物【%s】总血量%d，已被玩家打掉%d血。玩家【%s】当前血量%d，总血量%d。根据这个战况，让怪物说一句跟战斗相关的嘲讽的话，20字以内。",
                level.getMonsterName(), monsterMaxHp, monsterDamageTaken,
                pet.getName(), petCurrentHp, petMaxHp);
        return callAi(prompt);
    }

    @Override
    public String generateBattleSummary(Battle battle, Pet pet, Level level, int petCurrentHp) {
        String result = "VICTORY".equals(battle.getStatus()) ? "胜利" : "失败";
        int petMaxHp = pet.getHp();
        int currentRound = battle.getCurrentRound();

        String prompt = String.format(
                "战斗持续了%d回合。玩家%s系精灵【%s】%s了%s系怪物【%s】。玩家剩余血量%d/%d。用一段话总结这场战斗，30字以内。",
                currentRound, pet.getPetType(), pet.getName(), result,
                level.getMonsterType(), level.getMonsterName(), petCurrentHp, petMaxHp);
        return callAi(prompt);
    }

    @Override
    public String generateStrategy(Pet pet, Level level) {
        String prompt = String.format(
                "玩家%s系精灵【%s】（攻击力%d，防御力%d）要打%s系怪物【%s】（攻击力%d，防御力%d）。给一句策略建议，20字以内。",
                pet.getPetType(), pet.getName(), pet.getAttack(), pet.getDefense(),
                level.getMonsterType(), level.getMonsterName(), level.getMonsterAttack(), level.getMonsterDefense());
        return callAi(prompt);
    }
}