package com.game.korokingdom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_battle_log")
public class BattleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long battleId;      //第几场战斗
    private Integer roundNum;   //第几回合
    private Integer actionSeq;  //回合内的操作序号
    private String message;     //战斗日志

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}