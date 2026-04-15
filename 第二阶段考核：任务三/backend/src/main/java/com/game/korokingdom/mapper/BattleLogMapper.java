package com.game.korokingdom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.korokingdom.entity.BattleLog;
import org.apache.ibatis.annotations.Mapper;

//@用TableName注释自动映射表名，实体类就和数据库相当于相连接
@Mapper
public interface BattleLogMapper extends BaseMapper<BattleLog> {
}