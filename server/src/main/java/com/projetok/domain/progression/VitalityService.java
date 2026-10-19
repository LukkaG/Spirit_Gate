package com.projetok.domain.progression;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.race.Race;

/**
 * Único lugar que decide o HP máximo de uma entidade: nível (via StatsComponent) e itens
 * (via EquipmentComponent), sempre passados pela curva da raça (HpGrowth). Chamado depois
 * de criar a entidade, depois de level up e depois de equipar/desequipar — nunca em
 * nenhum outro lugar.
 */
public final class VitalityService {

    public void recalculate(GameEntity entity) {
        StatsComponent stats = entity.require(StatsComponent.class);
        Race race = entity.require(RaceComponent.class).race();
        int itemHpBonus = entity.get(EquipmentComponent.class)
                                 .map(EquipmentComponent::totalHpBonus)
                                 .orElse(0);

        int newMax = race.hpGrowth().computeMaxHp(stats.level(), itemHpBonus);
        entity.require(HealthComponent.class).recalculateMax(newMax);
    }
}
