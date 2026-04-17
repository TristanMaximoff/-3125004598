package com.game.korokingdom.service;

import com.game.korokingdom.entity.Battle;
import com.game.korokingdom.entity.Level;
import com.game.korokingdom.entity.Pet;

public interface AiService {

    String generateTaunt(Pet pet, Level level, int petCurrentHp, int monsterCurrentHp);

    String generateBattleSummary(Battle battle, Pet pet, Level level);

    String generateStrategy(Pet pet, Level level);
}