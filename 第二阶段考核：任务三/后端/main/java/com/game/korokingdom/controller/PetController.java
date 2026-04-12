package com.game.korokingdom.controller;

import com.game.korokingdom.dto.request.CreatePetReq;
import com.game.korokingdom.dto.response.PetResp;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final JwtUtil jwtUtil;

    // 创建精灵
    @PostMapping("/create")
    public Map<String, Object> createPet(@RequestHeader("Authorization") String authorization,
                                         @RequestBody CreatePetReq req) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        PetResp pet = petService.createPet(userId, req);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "精灵创建成功");
        result.put("data", pet);
        return result;
    }

    // 获取我的精灵
    @GetMapping("/my")
    public Map<String, Object> getMyPet(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        PetResp pet = petService.getMyPet(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", pet);
        return result;
    }

    // 获取精灵详情
    @GetMapping("/{petId}")
    public Map<String, Object> getPetInfo(@PathVariable Long petId) {
        PetResp pet = petService.getPetInfo(petId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", pet);
        return result;
    }
}