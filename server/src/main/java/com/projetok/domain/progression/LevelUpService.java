package com.projetok.domain.progression;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityLeveledUpEvent;
import com.projetok.domain.race.Race;

import java.util.Objects;

/**
 * Substitui a lógica hoje partida entre Entity.levelUp(...) e PlayerConfiguration — e
 * adiciona o bônus racial automático (ex.: Demônio +2 de força por nível, compensando a
 * fraqueza a dano Sagrado/Água).
 */
public final class LevelUpService {

    private final LevelingRules levelingRules;
    private final VitalityService vitalityService;

    public LevelUpService(LevelingRules levelingRules, VitalityService vitalityService) {
        this.levelingRules = Objects.requireNonNull(levelingRules, "levelingRules");
        this.vitalityService = Objects.requireNonNull(vitalityService, "vitalityService");
    }

    public void applyLevelUp(GameEntity entity, GameEventBus eventBus) {
        StatsComponent currentStats = entity.require(StatsComponent.class);
        Race race = entity.require(RaceComponent.class).race();

        int pointsGained = levelingRules.pointsForLevel(currentStats.level() + 1);
        StatsComponent newStats = currentStats.applyLevelUp(pointsGained, race.growthBonus());
        entity.attach(newStats);

        vitalityService.recalculate(entity); // novo nível pode mudar o HP máximo, mesmo sem item novo
        race.traits().forEach(trait -> trait.onLevelUp(entity));

        eventBus.publish(new EntityLeveledUpEvent(entity, newStats.level()));
    }
}
