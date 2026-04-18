package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_ai_call")
public class AiCall {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String callType;
    private LocalDateTime createdAt;
}