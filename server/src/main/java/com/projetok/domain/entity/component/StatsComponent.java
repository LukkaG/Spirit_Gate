package com.projetok.domain.entity.component;

import com.projetok.domain.race.AttributeGrowth;

/**
 * Sucessora dos campos strength/inteligency/agility soltos em Entity.java, mais o "points"
 * que hoje vive implícito. unspentPoints são os pontos livres que o jogador aloca manualmente
 * (spendOnX) — separados do bônus racial automático (applyLevelUp), que nunca é "gasto".
 */
public record StatsComponent(int strength, int intelligence, int agility, int energy, int level, int unspentPoints)
    implements Component {
    /** Mantém os construtores existentes com Energy inicial zero. */
    public StatsComponent(int strength,int intelligence,int agility,int level,int unspentPoints){this(strength,intelligence,agility,0,level,unspentPoints);}

    public StatsComponent {
        if (level < 1) throw new IllegalArgumentException("level deve ser >= 1");
        if (unspentPoints < 0) throw new IllegalArgumentException("unspentPoints não pode ser negativo");
        if (energy < 0 || energy > 10000) throw new IllegalArgumentException("Energy inválido");
    }

    /** pointsGained vem de LevelingRules; racialGrowth vem de Race.growthBonus() — automático, não gasto. */
    public StatsComponent applyLevelUp(int pointsGained, AttributeGrowth racialGrowth) {
        return new StatsComponent(
            strength + racialGrowth.strength(),
            intelligence + racialGrowth.intelligence(),
            agility + racialGrowth.agility(),
            energy,
            level + 1,
            unspentPoints + pointsGained
        );
    }

    public StatsComponent spendOnStrength() {
        requirePoints();
        return new StatsComponent(strength + 1, intelligence, agility, energy, level, unspentPoints - 1);
    }

    public StatsComponent spendOnIntelligence() {
        requirePoints();
        return new StatsComponent(strength, intelligence + 1, agility, energy, level, unspentPoints - 1);
    }

    public StatsComponent spendOnAgility() {
        requirePoints();
        return new StatsComponent(strength, intelligence, agility + 1, energy, level, unspentPoints - 1);
    }
    public StatsComponent spendOnEnergy(){requirePoints();return new StatsComponent(strength,intelligence,agility,energy+1,level,unspentPoints-1);}

    private void requirePoints() {
        if (unspentPoints <= 0) {
            throw new IllegalStateException("Sem pontos de atributo disponíveis para gastar.");
        }
    }
}
