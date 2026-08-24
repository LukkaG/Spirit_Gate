package com.projetok.domain.entity.capability;

import com.projetok.domain.race.DamageType;

public interface Attacker {
    int computeOutgoingDamage();
    DamageType outgoingDamageType();
}
