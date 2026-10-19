package com.projetok.domain.factory;

import com.projetok.domain.combat.CombatStyle;
import com.projetok.domain.item.Item;
import com.projetok.domain.race.Race;

/** "Molde" de criação de um Player. startingWeapon pode ser null — jogador começa desarmado. */
public record PlayerArchetype(
    Race race,
    int baseStrength,
    int baseIntelligence,
    int baseAgility,
    CombatStyle startingStyle,
    Item startingWeapon
) {
}
