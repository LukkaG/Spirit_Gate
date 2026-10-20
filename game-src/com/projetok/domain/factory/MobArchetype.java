package com.projetok.domain.factory;

import com.projetok.domain.ai.AiBehavior;
import com.projetok.domain.combat.CombatStyle;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.race.Race;

/** "Molde" de spawn de um Mob. */
public record MobArchetype(
    Race race,
    String displayName,
    int baseLevel,
    int baseStrength,
    int baseIntelligence,
    int baseAgility,
    CombatStyle combatStyle,
    LootTable lootTable,
    AiBehavior behavior
) {
}
