package com.projetok.domain.combat;

import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.race.DamageType;

/** Sucessora de SwordStyle.java — a ideia original estava certa, só faltava não travar em exceção. */
public interface CombatStyle {
    int computeDamage(StatsComponent stats);
    DamageType damageType();
    String displayName();
    int staminaCost();
}
