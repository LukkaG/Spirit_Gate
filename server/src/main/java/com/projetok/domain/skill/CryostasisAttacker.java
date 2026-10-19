package com.projetok.domain.skill;

import com.projetok.domain.entity.capability.Attacker;
import com.projetok.domain.race.DamageType;

/** Adaptador específico: dano base sem stats do ataque desarmado. */
public final class CryostasisAttacker implements Attacker {
    /** Retorna o dano aprovado por pulso. */
    @Override public int computeOutgoingDamage() { return 7; }
    /** Usa gelo, sem reutilizar afinidades de água. */
    @Override public DamageType outgoingDamageType() { return DamageType.ICE; }
}
