package com.projetok.domain.factory;

import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.AiComponent;
import com.projetok.domain.entity.component.CombatComponent;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.progression.VitalityService;

import java.util.Objects;

public final class MobFactory {

    private final VitalityService vitalityService;

    public MobFactory(VitalityService vitalityService) {
        this.vitalityService = Objects.requireNonNull(vitalityService, "vitalityService");
    }

    public Mob spawn(int id, MobArchetype archetype, Position position) {
        Mob mob = new Mob(id, archetype.displayName(), position, archetype.lootTable());

        mob.attach(new RaceComponent(archetype.race()));
        mob.attach(new StatsComponent(
            archetype.baseStrength(), archetype.baseIntelligence(), archetype.baseAgility(),
            archetype.baseLevel(), 0));
        mob.attach(new EquipmentComponent());
        mob.attach(new CombatComponent(archetype.combatStyle()));
        mob.attach(new AiComponent(archetype.behavior()));
        mob.attach(new HealthComponent(archetype.race().hpGrowth().baseHp()));

        vitalityService.recalculate(mob);
        return mob;
    }
}
