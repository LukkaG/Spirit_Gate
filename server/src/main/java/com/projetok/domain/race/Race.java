package com.projetok.domain.race;

import java.util.List;
import java.util.Map;

/**
 * Sucessora de Human.java/Demon.java como CLASSES. Raça agora é dado, não posição na
 * hierarquia de herança — trocar o tema do jogo (demônio -> espírito) é só trocar os dados
 * carregados aqui (ver RaceRegistry), sem tocar em Player, Mob, CombatService ou qualquer
 * outra classe de domínio.
 */
public final class Race {

    private final String id;
    private final String displayName;
    private final HpGrowth hpGrowth;
    private final AttributeGrowth growthBonus;
    private final Map<DamageType, Double> affinities;
    private final List<RacialTrait> traits;

    public Race(String id, String displayName, HpGrowth hpGrowth, AttributeGrowth growthBonus,
                Map<DamageType, Double> affinities, List<RacialTrait> traits) {
        this.id = id;
        this.displayName = displayName;
        this.hpGrowth = hpGrowth;
        this.growthBonus = growthBonus;
        this.affinities = Map.copyOf(affinities);
        this.traits = List.copyOf(traits);
    }

    /** 1.0 = neutro, acima de 1.0 = fraqueza (recebe mais dano), abaixo = resistência. */
    public double affinityFor(DamageType type) {
        return affinities.getOrDefault(type, 1.0);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public HpGrowth hpGrowth() {
        return hpGrowth;
    }

    public AttributeGrowth growthBonus() {
        return growthBonus;
    }

    public List<RacialTrait> traits() {
        return traits;
    }
}
