package com.projetok.domain.entity.component;

import com.projetok.domain.race.AttributeGrowth;

/**
 * Sucessora dos campos strength/inteligency/agility soltos em Entity.java, mais o "points"
 * que hoje vive implícito. unspentPoints são os pontos livres que o jogador aloca manualmente
 * (spendOnX) — separados do bônus racial automático (applyLevelUp), que nunca é "gasto".
 */
public record StatsComponent(int strength, int intelligence, int agility, int level, int unspentPoints)
    implements Component {

    public StatsComponent {
        if (level < 1) throw new IllegalArgumentException("level deve ser >= 1");
        if (unspentPoints < 0) throw new IllegalArgumentException("unspentPoints não pode ser negativo");
    }

    /** pointsGained vem de LevelingRules; racialGrowth vem de Race.growthBonus() — automático, não gasto. */
    public StatsComponent applyLevelUp(int pointsGained, AttributeGrowth racialGrowth) {
        return new StatsComponent(
            strength + racialGrowth.strength(),
            intelligence + racialGrowth.intelligence(),
            agility + racialGrowth.agility(),
            level + 1,
            unspentPoints + pointsGained
        );
    }

    public StatsComponent spendOnStrength() {
        requirePoints();
        return new StatsComponent(strength + 1, intelligence, agility, level, unspentPoints - 1);
    }

    public StatsComponent spendOnIntelligence() {
        requirePoints();
        return new StatsComponent(strength, intelligence + 1, agility, level, unspentPoints - 1);
    }

    public StatsComponent spendOnAgility() {
        requirePoints();
        return new StatsComponent(strength, intelligence, agility + 1, level, unspentPoints - 1);
    }

    private void requirePoints() {
        if (unspentPoints <= 0) {
            throw new IllegalStateException("Sem pontos de atributo disponíveis para gastar.");
        }
    }
}
