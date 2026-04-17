package com.game.korokingdom.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiChatReq {
    private String model;
    private List<Map<String, String>> messages;
    private Boolean stream;
}