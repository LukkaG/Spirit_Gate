package com.projetok.domain.combat.style;

import com.projetok.domain.combat.CombatStyle;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.race.DamageType;

/**
 * Reescrita de WaterStyle.java original. O bug antigo (executeAttack -> damagetaken() ->
 * throw new UnsupportedOperationException) foi removido: o estilo só CALCULA dano, nunca
 * causa efeito colateral sozinho — quem aplica dano de fato é sempre o CombatService.
 */
public final class WaterStyle implements CombatStyle {

    private static final int STAMINA_COST = 20;
    private static final int BASE_DAMAGE = 50;

    @Override
    public int computeDamage(StatsComponent stats) {
        return BASE_DAMAGE + com.projetok.domain.combat.AttributeCombatRules.damageBonus(com.projetok.domain.combat.AttributeCombatRules.DamageKind.DOMAIN_SKILL,stats.strength(),stats.intelligence());
    }

    @Override
    public DamageType damageType() {
        return DamageType.WATER;
    }

    @Override
    public String displayName() {
        return "Arte da Água";
    }

    @Override
    public int staminaCost() {
        return STAMINA_COST;
    }
}
