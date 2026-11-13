package com.projetok.rpg;

import com.projetok.domain.mob.MobCatalog;
import com.projetok.domain.movement.CollisionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Confere a adaptação do catálogo real sem substituir IA ou recompensas do runtime. */
class MobCatalogRuntimeTest {
    /** Cada espécie real conserva parâmetros; só o encontro aplica seu piso explícito de HP. */
    @Test void everySpeciesKeepsItsParametersAndInstanceIdentity() {
        var catalog = MobCatalog.fromProperties(RpgSceneSet.entries("enemies"));
        var content = RpgContent.builtin();
        var space = new CollisionService.Space(960, 640, List.of(), List.of());
        for (var definition : catalog.all()) {
            String region = content.get("enemy", definition.id()).region();
            var spawns = Map.of("org02_spawn", definition.id() + "|" + region + "|100|100");
            for (int floor : new int[]{1, 10000}) {
                var population = new EnemyService(content, space, spawns, ignored -> space, floor);
                var enemy = population.current(region).get(0);
                var runtime = enemy.definition;
                assertEquals(definition.id(), runtime.id());
                assertEquals(Math.max(floor, definition.hp()), runtime.hp());
                assertEquals(runtime.hp(), enemy.hp);
                assertEquals(definition.attack(), runtime.attack());
                assertEquals(definition.speed(), runtime.speed());
                assertEquals(definition.aggro(), runtime.aggro());
                assertEquals(definition.leash(), runtime.leash());
                assertEquals(definition.respawn(), runtime.respawn());
                assertEquals(definition.level(), runtime.level());
                assertEquals("org02_spawn", enemy.spawn);
                assertEquals(1, enemy.generation);
                assertEquals(enemy.home, enemy.getPosition());
                assertEquals(100, enemy.getId());
            }
        }
    }
}
