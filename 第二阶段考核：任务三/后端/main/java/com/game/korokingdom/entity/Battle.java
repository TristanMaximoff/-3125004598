package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_battle")
public class Battle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private Integer levelId;
    private String battleState;
    private Integer currentRound;
    private LocalDateTime lastActionTime;
    private String status;
    private Boolean rewardClaimed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}