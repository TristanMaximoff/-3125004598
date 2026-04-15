package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_battle")
public class Battle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;                     //目前战斗精灵
    private Integer levelId;                //关卡数
    private String battleState;             //战斗状态快照（JSON），用于断线重连
    //精灵ID、精灵当前血量、精灵技能PP剩余、怪物当前血量、怪物技能PP剩余、增益状态、回合序列号、战斗日志
    private Integer currentRound;           //回合数
    private LocalDateTime lastActionTime;   //上次行动时间，用于防脚本（最短出招间隔300ms）
    private String status;                  //战斗状态：ACTIVE/VICTORY/DEFEAT/SURRENDER
    private Boolean rewardClaimed;          //奖励领取状态

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}