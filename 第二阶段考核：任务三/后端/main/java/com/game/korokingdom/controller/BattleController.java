package com.game.korokingdom.controller;

import com.game.korokingdom.dto.request.BattleActionReq;
import com.game.korokingdom.dto.response.BattleResp;
import com.game.korokingdom.service.BattleService;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.service.LevelService;
import com.game.korokingdom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/battle")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;
    private final PetService petService;
    private final LevelService levelService;
    private final JwtUtil jwtUtil;

    // 开始战斗
    @PostMapping("/start")
    public Map<String, Object> startBattle(@RequestHeader("Authorization") String authorization,
                                           @RequestParam Integer levelId) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();

        BattleResp battle = battleService.startBattle(petId, levelId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "战斗开始");
        result.put("data", battle);
        return result;
    }

    // 执行行动
    @PostMapping("/action")
    public Map<String, Object> executeAction(@RequestHeader("Authorization") String authorization,
                                             @RequestParam Long battleId,
                                             @RequestBody BattleActionReq req) {
        String token = authorization.replace("Bearer ", "");
        jwtUtil.getUserIdFromToken(token); // 校验token有效性

        BattleResp battle = battleService.executeAction(battleId, req);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", battle);
        return result;
    }

    // 投降
    @PostMapping("/surrender")
    public Map<String, Object> surrender(@RequestHeader("Authorization") String authorization,
                                         @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");
        jwtUtil.getUserIdFromToken(token);

        BattleResp battle = battleService.surrender(battleId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已投降");
        result.put("data", battle);
        return result;
    }

    // 获取战斗状态（断线重连用）
    @GetMapping("/state")
    public Map<String, Object> getBattleState(@RequestHeader("Authorization") String authorization,
                                              @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");
        jwtUtil.getUserIdFromToken(token);

        BattleResp battle = battleService.getBattleState(battleId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", battle);
        return result;
    }

    // 恢复战斗
    @PostMapping("/resume")
    public Map<String, Object> resumeBattle(@RequestHeader("Authorization") String authorization,
                                            @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");
        jwtUtil.getUserIdFromToken(token);

        BattleResp battle = battleService.resumeBattle(battleId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", battle);
        return result;
    }
}