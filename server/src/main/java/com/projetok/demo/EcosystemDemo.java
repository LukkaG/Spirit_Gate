package com.projetok.demo;

import com.projetok.domain.ai.PassiveBehavior;
import com.projetok.domain.combat.CombatService;
import com.projetok.domain.combat.style.WaterStyle;
import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityDeathEvent;
import com.projetok.domain.factory.MobArchetype;
import com.projetok.domain.factory.MobFactory;
import com.projetok.domain.factory.PlayerArchetype;
import com.projetok.domain.factory.PlayerFactory;
import com.projetok.domain.item.Item;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.progression.LevelUpService;
import com.projetok.domain.progression.LevelingRules;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.Race;
import com.projetok.domain.race.RaceRegistry;

/**
 * Smoke test manual de como as peças se encaixam — NÃO é o Bootstrap de produção (esse
 * depende do GameLoop e da camada de rede, que vêm no passo 2 do roadmap). Rode com:
 *   javac -d out $(find src -name "*.java") && java -cp out com.projetok.demo.EcosystemDemo
 */
public final class EcosystemDemo {

    public static void main(String[] args) {
        RaceRegistry raceRegistry = RaceRegistry.withDefaults();
        GameEventBus eventBus = new GameEventBus();
        VitalityService vitalityService = new VitalityService();
        CombatService combatService = new CombatService(eventBus);

        eventBus.subscribe(EntityDeathEvent.class, event ->
            System.out.println("[evento] " + event.entity().getName() + " morreu."));

        Race human = raceRegistry.get("human");
        Race demon = raceRegistry.get("demon");

        PlayerArchetype swordsman = new PlayerArchetype(
            human, 10, 5, 8, new WaterStyle(), Item.weapon("iron_sword", "Espada de Ferro", 15));
        Player player = new PlayerFactory(vitalityService)
            .create(1, "Lukka", swordsman, new Position(0, 0), noOpConnection());

        MobArchetype demonGrunt = new MobArchetype(
            demon, "Demônio Menor", 5, 15, 5, 8, new WaterStyle(),
            LootTable.of(Item.misc("demon_horn", "Chifre de Demônio")), new PassiveBehavior());
        Mob mob = new MobFactory(vitalityService).spawn(2, demonGrunt, new Position(1, 0));

        System.out.println(player.getName() + " HP: " + hp(player));
        System.out.println(mob.getName() + " HP: " + hp(mob) + " (demônio: +30% de dano de Água)");

        int damage = combatService.attack(player, mob);
        System.out.println("Dano causado (afinidade de raça já aplicada): " + damage);
        System.out.println(mob.getName() + " HP após o ataque: " + hp(mob));

        LevelUpService levelUpService = new LevelUpService(new LevelingRules(), vitalityService);
        levelUpService.applyLevelUp(player, eventBus);
        System.out.println(player.getName() + " subiu de nível — HP atual após recalcular: " + hp(player));
    }

    private static int hp(GameEntity entity) {
        return entity.require(HealthComponent.class).current();
    }

    private static PlayerConnection noOpConnection() {
        return new PlayerConnection() {
            @Override
            public void send(Object packet) {
                // implementação real fica no adaptador Kryonet, passo 2 do roadmap
            }

            @Override
            public void disconnect() {
            }
        };
    }
}
