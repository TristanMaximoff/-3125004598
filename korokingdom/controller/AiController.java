package com.game.korokingdom.controller;

import com.game.korokingdom.annotation.RequireLogin;
import com.game.korokingdom.entity.Battle;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.entity.Pet;
import com.game.korokingdom.service.AiService;
import com.game.korokingdom.service.BattleService;
import com.game.korokingdom.service.LevelService;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final PetService petService;
    private final LevelService levelService;
    private final BattleService battleService;
    private final JwtUtil jwtUtil;

    @GetMapping("/taunt")
    @RequireLogin
    public Map<String, Object> getTaunt(@RequestHeader("Authorization") String authorization,
                                        @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");  //取出纯token
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        Pet pet = petService.getById(petId);

        Battle battle = battleService.getById(battleId);
        Level level = levelService.getById(battle.getLevelId());

        String taunt = aiService.generateTaunt(pet, level, 0, 0);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", taunt);
        return result;
    }

    @GetMapping("/summary")
    @RequireLogin
    public Map<String, Object> getSummary(@RequestHeader("Authorization") String authorization,
                                          @RequestParam Long battleId) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        Pet pet = petService.getById(petId);

        Battle battle = battleService.getById(battleId);
        Level level = levelService.getById(battle.getLevelId());

        String summary = aiService.generateBattleSummary(battle, pet, level);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", summary);
        return result;
    }

    @GetMapping("/strategy")
    @RequireLogin
    public Map<String, Object> getStrategy(@RequestHeader("Authorization") String authorization,
                                           @RequestParam Integer levelId) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long petId = petService.getMyPet(userId).getId();
        Pet pet = petService.getById(petId);

        Level level = levelService.getById(levelId);

        String strategy = aiService.generateStrategy(pet, level);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", strategy);
        return result;
    }
}