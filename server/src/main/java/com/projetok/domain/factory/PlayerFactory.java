package com.projetok.domain.factory;

import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.CombatComponent;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.InventoryComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.item.EquipmentSlot;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.progression.VitalityService;

import java.util.Objects;

/**
 * Único lugar que monta um Player. Diferente do Human.java original (onde sword/style
 * nunca eram atribuídos por engano, silenciosamente), aqui é estruturalmente impossível
 * esquecer um componente: se StatsComponent, CombatComponent ou HealthComponent não forem
 * anexados, qualquer chamada a require(...) falha imediatamente e ruidosamente.
 */
public final class PlayerFactory {

    private final VitalityService vitalityService;

    public PlayerFactory(VitalityService vitalityService) {
        this.vitalityService = Objects.requireNonNull(vitalityService, "vitalityService");
    }

    public Player create(int id, String username, PlayerArchetype archetype, Position spawn,
                          PlayerConnection connection) {
        Player player = new Player(id, username, spawn, connection);

        player.attach(new RaceComponent(archetype.race()));
        player.attach(new StatsComponent(
            archetype.baseStrength(), archetype.baseIntelligence(), archetype.baseAgility(), 1, 0));
        player.attach(new EquipmentComponent());
        player.attach(new InventoryComponent());
        player.attach(new CombatComponent(archetype.startingStyle()));
        player.attach(new HealthComponent(archetype.race().hpGrowth().baseHp()));

        if (archetype.startingWeapon() != null) {
            player.require(EquipmentComponent.class).equip(EquipmentSlot.WEAPON, archetype.startingWeapon());
        }

        vitalityService.recalculate(player); // única chamada que define o HP máximo real
        return player;
    }
}
