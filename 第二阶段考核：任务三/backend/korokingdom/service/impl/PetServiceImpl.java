package com.game.korokingdom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.game.korokingdom.dto.request.CreatePetReq;
import com.game.korokingdom.dto.response.PetResp;
import com.game.korokingdom.entity.Pet;
import com.game.korokingdom.entity.User;
import com.game.korokingdom.exception.BusinessException;
import com.game.korokingdom.mapper.PetMapper;
import com.game.korokingdom.service.PetService;
import com.game.korokingdom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements PetService {

    private final UserService userService;

    //各系别初始宠物属性
    private int[] getBaseStats(String petType) {
        switch (petType) {
            case "GRASS":  // 喵喵
                return new int[]{150, 80, 120, 80};
            case "FIRE":   // 火花
                return new int[]{100, 150, 80, 120};
            case "WATER":  // 水蓝蓝
                return new int[]{125, 100, 100, 100};
            default:
                throw new BusinessException("无效的系别");
        }
    }

    @Override
    public PetResp createPet(Long userId, CreatePetReq req) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pet::getUserId, userId);             //把这个要创建精灵的用户id和数据库里每个精灵的主人id对比
        if (this.baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("你已拥有精灵，不能重复创建");
        }

        Random random = new Random();
        int individualValue = random.nextInt(16);   //0-15

        int[] baseStats = getBaseStats(req.getPetType());
        int baseHp = baseStats[0];
        int baseAttack = baseStats[1];
        int baseDefense = baseStats[2];
        int baseSpeed = baseStats[3];
        //根据系别取出精灵属性，赋给base再和个体值相加

        int finalHp = baseHp + individualValue;
        int finalAttack = baseAttack + individualValue;
        int finalDefense = baseDefense + individualValue;
        int finalSpeed = baseSpeed + individualValue;

        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setName(req.getName());
        pet.setPetType(req.getPetType());
        pet.setLevel(1);
        pet.setExp(0);
        pet.setHp(finalHp);
        pet.setAttack(finalAttack);
        pet.setDefense(finalDefense);
        pet.setSpeed(finalSpeed);
        pet.setIndividualValue(individualValue);
        pet.setCurrentHp(finalHp);
        pet.setCurrentLevel(1);
        pet.setCreatedAt(LocalDateTime.now());

        this.baseMapper.insert(pet);

        return toPetResp(pet);
    }

    @Override
    public PetResp getPetInfo(Long petId) {
        Pet pet = this.baseMapper.selectById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }
        return toPetResp(pet);
    }

    @Override
    public PetResp getMyPet(Long userId) {
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pet::getUserId, userId);
        Pet pet = this.baseMapper.selectOne(wrapper);
        if (pet == null) {
            throw new BusinessException("你还没有精灵，请先创建");
        }
        return toPetResp(pet);
    }

    @Override
    public void addExp(Long petId, int exp) {
        Pet pet = this.baseMapper.selectById(petId);
        if (pet == null) {
            throw new BusinessException("精灵不存在");
        }
        pet.setExp(pet.getExp() + exp);
        this.baseMapper.updateById(pet);  // 先保存经验
        checkLevelUp(petId);              // 再检查升级
    }

    @Override
    public void checkLevelUp(Long petId) {
        while (true) {
            // 查询最新数据
            Pet pet = this.baseMapper.selectById(petId);
            if (pet == null) {
                return;
            }

            int needExp = 100 + (pet.getLevel() - 1) * 50;
            if (pet.getExp() >= needExp) {
                // 直接用 LambdaUpdateWrapper 更新，避免缓存问题
                LambdaUpdateWrapper<Pet> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Pet::getId, petId);
                wrapper.setSql("exp = exp - " + needExp);
                wrapper.setSql("level = level + 1");
                wrapper.setSql("hp = hp + 15");
                wrapper.setSql("attack = attack + 5");
                wrapper.setSql("defense = defense + 4");
                wrapper.setSql("current_hp = hp");
                wrapper.setSql("updated_at = NOW()");
                this.baseMapper.update(null, wrapper);
                // 继续循环，可能连升多级
            } else {
                break;
            }
        }
    }
    //根据dto里的字段把pet转换成resp
    private PetResp toPetResp(Pet pet) {
        PetResp resp = new PetResp();
        resp.setId(pet.getId());
        resp.setName(pet.getName());
        resp.setPetType(pet.getPetType());
        resp.setLevel(pet.getLevel());
        resp.setExp(pet.getExp());
        resp.setHp(pet.getHp());
        resp.setAttack(pet.getAttack());
        resp.setDefense(pet.getDefense());
        resp.setIndividualValue(pet.getIndividualValue());
        resp.setCurrentHp(pet.getCurrentHp());
        resp.setCurrentLevel(pet.getCurrentLevel());
        return resp;
    }
}