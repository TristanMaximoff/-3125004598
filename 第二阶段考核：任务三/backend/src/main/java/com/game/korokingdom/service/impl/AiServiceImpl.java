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