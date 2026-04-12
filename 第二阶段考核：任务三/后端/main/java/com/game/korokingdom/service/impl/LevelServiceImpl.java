package com.game.korokingdom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.game.korokingdom.dto.response.LevelResp;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.entity.Pet;
import com.game.korokingdom.exception.BusinessException;
import com.game.korokingdom.mapper.LevelMapper;
import com.game.korokingdom.service.LevelService;
import com.game.korokingdom.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelServiceImpl extends ServiceImpl<LevelMapper, Level> implements LevelService {

    private final PetService petService;

    @Override
    public List<LevelResp> getAllLevels() {
        List<Level> levels = this.baseMapper.selectList(new LambdaQueryWrapper<Level>().orderByAsc(Level::getLevelNum));
        return levels.stream().map(this::toLevelResp).collect(Collectors.toList());
    }

    @Override
    public LevelResp getCurrentLevel(Long petId) {
        Pet pet = petService.getById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }

        int currentLevelNum = pet.getCurrentLevel();
        Level level = this.baseMapper.selectOne(
                new LambdaQueryWrapper<Level>().eq(Level::getLevelNum, currentLevelNum)
        );
        if (level == null) {
            throw new BusinessException("当前关卡不存在");
        }
        return toLevelResp(level);
    }

    @Override
    public LevelResp getNextLevel(Long petId) {
        Pet pet = petService.getById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }

        int nextLevelNum = pet.getCurrentLevel() + 1;
        Level level = this.baseMapper.selectOne(
                new LambdaQueryWrapper<Level>().eq(Level::getLevelNum, nextLevelNum)
        );
        if (level == null) {
            return null;
        }
        return toLevelResp(level);
    }

    @Override
    public void unlockNextLevel(Long petId) {
        Pet pet = petService.getById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }

        int nextLevelNum = pet.getCurrentLevel() + 1;
        Level nextLevel = this.baseMapper.selectOne(
                new LambdaQueryWrapper<Level>().eq(Level::getLevelNum, nextLevelNum)
        );
        if (nextLevel == null) {
            throw new BusinessException("已经是最后一关");
        }

        pet.setCurrentLevel(nextLevelNum);
        petService.updateById(pet);
    }

    private LevelResp toLevelResp(Level level) {
        LevelResp resp = new LevelResp();
        resp.setId(level.getId());
        resp.setLevelNum(level.getLevelNum());
        resp.setName(level.getName());
        resp.setMonsterName(level.getMonsterName());
        resp.setMonsterType(level.getMonsterType());
        resp.setMonsterHp(level.getMonsterHp());
        resp.setMonsterAttack(level.getMonsterAttack());
        resp.setMonsterDefense(level.getMonsterDefense());
        resp.setExpReward(level.getExpReward());
        resp.setFirstExpBonus(level.getFirstExpBonus());
        return resp;
    }
}