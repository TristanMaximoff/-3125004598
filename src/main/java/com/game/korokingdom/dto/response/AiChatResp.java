package com.game.korokingdom.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiChatResp {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Map<String, String> message;
    }
}