package com.game.korokingdom.controller;

import com.game.korokingdom.dto.request.LoginReq;
import com.game.korokingdom.dto.request.RegisterReq;
import com.game.korokingdom.dto.response.UserResp;
import com.game.korokingdom.service.UserService;
import com.game.korokingdom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // 注册
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterReq req) {
        String token = userService.register(req);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功");
        result.put("token", token);
        return result;
    }

    // 登录
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        String token = userService.login(req);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("token", token);
        return result;
    }

    // 获取个人信息（需要登录）
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserResp userInfo = userService.getUserInfo(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", userInfo);
        return result;
    }

    // 修改昵称
    @PutMapping("/nickname")
    public Map<String, Object> updateNickname(@RequestHeader("Authorization") String authorization,
                                              @RequestParam String nickname) {
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        userService.updateNickname(userId, nickname);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "修改成功");
        return result;
    }
}