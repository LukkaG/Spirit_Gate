package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.multiplayer.Protocol;
import com.projetok.multiplayer.RpgSessionWorld;
import com.projetok.studio.WorldContent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica resistência no multiplayer, dano real e preservação do combate legado. */
class EnemyDurabilityTest {
    @Test void contentMultiplayerPublishesFullHealthWithinExistingProtocolLimits() throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "human"), null, true);
        assertEquals(40, world.combatSnapshot().session().world().actors().get(0).hp());
        try {
            world.enableRevisions(WorldContent.baseline());
            var snapshot = world.combatSnapshot();
            var enemy = snapshot.session().world().actors().get(0);
            assertEquals(10_000, enemy.hp());
            assertEquals(10_000, enemy.maxHp());
            var encoded = new ByteArrayOutputStream();
            Protocol.write(encoded, snapshot);
            assertEquals(snapshot, Protocol.read(new ByteArrayInputStream(encoded.toByteArray())));
            assertTrue(encoded.size() < Protocol.MAX_FRAME);
        } finally {
            world.closed();
        }
        var legacy = new RpgSessionWorld(List.of("reaper", "human"), null, true);
        assertEquals(40, legacy.combatSnapshot().session().world().actors().get(0).hp());
    }

    @Test void skillDamagesDurableEnemyAndDeathRespawnsAtFullHealth() {
        var space = new CollisionService.Space(960, 640, List.of(), List.of());
        var definitions = Map.of("target", "slime|forest|220|300");
        var encounter = new SharedRpgEncounter(Map.of("forest", space), definitions,
                SharedRpgEncounter.RewardPolicy.DISABLED, 10_000);
        var scenes = new RpgSceneSet("forest",
                Map.of("forest", new RpgSceneSet.Geometry(space, new Position(140, 300))),
                Map.of(), Map.of(), Map.of(), Map.of());
        var world = new RpgWorld(CharacterIdentity.create("Resistencia", "reaper", "azure"), scenes);
        world.useNetworkFootprint();
        world.awardExperience(RpgProgression.builtin().threshold(15));
        assertTrue(world.chooseDomain("water"));
        encounter.join(1, world);
        var enemy = world.skillEnemies().get(0);
        var engine = new RpgSkillEngine();
        var players = Map.of(1, world);
        assertTrue(engine.cast(1, world, new RpgSkillEngine.Cast(1, 1_000_100,
                enemy.generation, 220, 300), players));
        for (int tick = 1; tick <= 10; tick++) {
            world.tick(0, 0);
            encounter.step(tick);
            engine.clock(tick);
            engine.tick(players);
        }
        assertTrue(enemy.hp > 9_000 && enemy.hp < 10_000, "A skill causa dano sem derrotar o alvo");

        // A troca de revisão conserva o dano recebido e a vida máxima do encontro.
        var copy = encounter.fork(Map.of("forest", space), definitions);
        assertEquals(encounter.enemies(10), copy.enemies(10));
        assertEquals(10_000, copy.enemies(10).get(0).maxHp());

        int remaining = enemy.hp;
        assertEquals(remaining, world.skillDamage(enemy, enemy.generation, 10_000));
        assertEquals(0, enemy.hp);
        assertEquals(0, world.skillDamage(enemy, enemy.generation, 10_000));
        assertEquals(1, encounter.events().stream().filter(e -> e.kind().equals("DEATH")).count());
        encounter.leave(world);
        encounter.step(250);
        assertEquals(10_000, enemy.hp);
        assertEquals(2, enemy.generation);
    }

    @Test void newSpawnsKeepDurabilityAfterRevisionChange() {
        var space = new CollisionService.Space(960, 640, List.of(), List.of());
        var regions = Map.of("forest", space);
        var encounter = new SharedRpgEncounter(regions, Map.of("old", "slime|forest|220|300"),
                SharedRpgEncounter.RewardPolicy.DISABLED, 10_000);
        var copy = encounter.fork(regions, Map.of("new", "slime|forest|320|300"));
        assertEquals(10_000, copy.enemies(0).get(0).hp());
        assertEquals(10_000, copy.enemies(0).get(0).maxHp());
    }
}
