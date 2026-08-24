package com.projetok.domain.entity.capability;

import com.projetok.domain.race.Race;

public interface Damageable {
    void receiveDamage(int amount);
    boolean isDead();
    Race race();
}
