package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_battle_log")
public class BattleLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long battleId;
    private Integer roundNum;
    private Integer actionSeq;
    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}