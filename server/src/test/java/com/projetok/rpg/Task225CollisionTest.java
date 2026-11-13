package com.projetok.rpg;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões isoladas de contato da TASK-225, com posições dos mobs controladas pelo cenário. */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class Task225CollisionTest {
    /** Usa o executor e a geometria reais; a IA fica fora destes ensaios de colisão determinísticos. */
    private static final class Fixture {
        final RpgSkillEngine engine = new RpgSkillEngine();
        final SharedRpgEncounter encounter;
        final RpgWorld world;
        final Map<Integer, RpgWorld> players = new LinkedHashMap<>();
        long tick;

        Fixture(String kit, List<CollisionService.Obstacle> obstacles, Map<String, String> definitions) {
            var space = new CollisionService.Space(960, 640, obstacles, List.of());
            var scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space,
                    new Position(140, 300))), definitions, Map.of(), Map.of(), Map.of());
            String race = switch (kit) {
                case "water", "wind", "thunder", "fire" -> "reaper";
                case "spirit" -> "spirit";
                case "aetheri" -> "aetheri";
                default -> "human";
            };
            world = new RpgWorld(CharacterIdentity.create("Contato", race, "azure"), scenes);
            world.useNetworkFootprint();
            world.awardExperience(RpgProgression.builtin().threshold(15));
            assertTrue(world.chooseDomain(switch (kit) {
                case "aetheri" -> "bow_long";
                case "spirit" -> "darkness_control";
                default -> kit;
            }));
            encounter = new SharedRpgEncounter(Map.of("forest", space), definitions,
                    SharedRpgEncounter.RewardPolicy.DISABLED);
            encounter.join(1, world);
            players.put(1, world);
            world.skillEnemies().forEach(enemy -> enemy.hp = 1000);
        }

        /** Lança sobre a identidade escolhida, sem converter a posição da mira em posição autoritativa. */
        boolean cast(int slot, EnemyService.Enemy target) {
            engine.clock(tick);
            return engine.cast(1, world, new RpgSkillEngine.Cast(slot,
                    target == null ? 0 : 1_000_000 + target.getId(),
                    target == null ? 0 : target.generation, 200, 300), players);
        }

        /** Só avança relógios e skills; cada teste controla explicitamente o movimento adversário. */
        void advance(int count) {
            for (int index = 0; index < count; index++) {
                tick++;
                players.values().forEach(actor -> actor.tick(0, 0));
                engine.clock(tick);
                engine.tick(players);
            }
        }

        /** Repetir o executor no mesmo tick não representa um novo quadro autoritativo. */
        void repeatTick() { for (int repeat = 0; repeat < 8; repeat++) engine.tick(players); }

        EnemyService.Enemy enemy(int index) { return world.skillEnemies().get(index); }

        /** Publica um aliado sólido na rota com o mesmo bloco físico usado pelo multiplayer. */
        RpgWorld ally(float x, float y) {
            var space = new CollisionService.Space(960, 640, List.of(), List.of());
            var scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space,
                    new Position(x, y))), Map.of(), Map.of(), Map.of(), Map.of());
            var ally = new RpgWorld(CharacterIdentity.create("Aliado", "human", "azure"), scenes);
            ally.useNetworkFootprint();
            encounter.join(2, ally);
            players.put(2, ally);
            world.setSyntheticBodies(List.of(CollisionService.Body.block(
                    new GameEntity(2, "Aliado", ally.skillPosition()) {}, ally.physicalRadius())));
            return ally;
        }
    }

    // --- Primeiro contato e cancelamento da identidade original ---

    @TestFactory Stream<DynamicTest> directedProjectilesStopWhenMovingTargetsCrossBehindThinWalls() {
        return Stream.of("water", "wind", "thunder", "fire", "aetheri").map(kit ->
                DynamicTest.dynamicTest(kit + " — parede após o lançamento", () -> {
                    var f = new Fixture(kit, List.of(new CollisionService.Obstacle(240, 250, 1, 100)),
                            Map.of("a_target", "slime|forest|220|300"));
                    assertTrue(f.cast(1, f.enemy(0)));
                    f.advance(1);
                    f.enemy(0).setPosition(new Position(280, 300));
                    f.advance(25);
                    assertEquals(1000, f.enemy(0).hp);
                    assertTrue(f.encounter.events().stream().noneMatch(event -> event.kind().equals("HIT")));
                    var impact = f.engine.events().stream().filter(event -> event.fromX() == event.x()
                            && event.fromY() == event.y()).findFirst().orElseThrow();
                    assertEquals(236, impact.x(), .01);
                    assertEquals(300, impact.y(), .01);
                }));
    }

    @Test void closeBodiesAreOrderedByFirstIntersectionRatherThanSelectedTargetOrId() {
        var f = new Fixture("water", List.of(), Map.of(
                "a_selected", "slime|forest|300|300", "b_offset", "slime|forest|200|319",
                "c_first", "slime|forest|200|300"));
        assertTrue(f.cast(1, f.enemy(0)));
        f.advance(20);
        assertEquals(1000, f.enemy(0).hp);
        assertEquals(1000, f.enemy(1).hp);
        assertEquals(980, f.enemy(2).hp);
        var impact = f.engine.events().stream().filter(event -> event.fromX() == event.x()
                && event.fromY() == event.y()).findFirst().orElseThrow();
        assertEquals(177, impact.x(), .01);
    }

    @Test void coincidentContactsHaveAStableIdTieBreakAndOnlyOneVictim() {
        var f = new Fixture("thunder", List.of(), Map.of(
                "a_first", "slime|forest|220|300", "b_second", "slime|forest|220|300"));
        assertTrue(f.cast(2, null));
        f.advance(20);
        assertEquals(976, f.enemy(0).hp);
        assertEquals(1000, f.enemy(1).hp);
        assertEquals(1, f.encounter.events().stream().filter(event -> event.kind().equals("HIT")).count());
    }

    @Test void deathOrGenerationChangeCancelsAFlightBeforeAnInterveningBodyCanInheritIt() {
        for (boolean respawn : List.of(false, true)) {
            var f = new Fixture("water", List.of(), Map.of(
                    "a_selected", "slime|forest|300|300", "b_blocker", "slime|forest|220|300"));
            assertTrue(f.cast(1, f.enemy(0)));
            f.advance(1);
            if (respawn) f.enemy(0).generation++; else f.enemy(0).hp = 0;
            f.advance(20);
            assertEquals(1000, f.enemy(1).hp);
            assertTrue(f.encounter.events().stream().noneMatch(event -> event.kind().equals("HIT")));
            assertTrue(f.engine.events().stream().allMatch(event -> event.fromX() != event.x()
                    || event.fromY() != event.y()));
        }
    }

    @Test void targetProjectilesCannotBeLaunchedThroughAnExistingWall() {
        var f = new Fixture("water", List.of(new CollisionService.Obstacle(240, 250, 1, 100)),
                Map.of("a_target", "slime|forest|300|300"));
        int energy = f.world.networkState().stamina();
        assertFalse(f.cast(1, f.enemy(0)));
        assertEquals(energy, f.world.networkState().stamina());
        assertTrue(f.engine.events().isEmpty());
    }

    // --- Semântica de contadores e reexecução do mesmo tick ---

    @Test void multipleFamiliesKeepTheirOwnHitBudgetsWhenTicksAreRepeated() {
        record Scenario(String kit, int slot, int steps, int directHits, int directAmount) { }
        var scenarios = List.of(new Scenario("wind", 2, 24, 3, 10),
                new Scenario("thunder", 3, 30, 4, -1),
                new Scenario("aetheri", 3, 60, 5, 10),
                new Scenario("aetheri", 5, 20, 1, 28),
                new Scenario("fire", 5, 60, 5, 10),
                new Scenario("martial", 2, 16, 4, 9),
                new Scenario("martial", 5, 24, 6, 8));
        for (var scenario : scenarios) {
            var f = new Fixture(scenario.kit(), List.of(), Map.of("a_target", "slime|forest|200|300"));
            assertTrue(f.cast(scenario.slot(), f.enemy(0)), scenario.toString());
            for (int index = 0; index < scenario.steps(); index++) {
                f.advance(1);
                f.repeatTick();
            }
            var hits = f.encounter.events().stream().filter(event -> event.kind().equals("HIT"))
                    .filter(event -> scenario.directAmount() < 0 || event.amount() == scenario.directAmount()).toList();
            assertEquals(scenario.directHits(), hits.size(), scenario.toString());
            if (scenario.kit().equals("thunder"))
                assertEquals(List.of(20, 15, 11, 8), hits.stream().map(SharedRpgEncounter.Event::amount).toList());
            if (scenario.kit().equals("aetheri") && scenario.slot() == 5)
                assertEquals(972, f.enemy(0).hp);
        }
    }

    // --- Investidas, obstáculos, aliados e bordas ---

    @TestFactory Stream<DynamicTest> allThreeDashesStopBeforeThinWallsAndCannotHitBehindThem() {
        return Stream.of("water", "wind", "thunder").map(kit ->
                DynamicTest.dynamicTest(kit + " — investida e parede fina", () -> {
                    var f = new Fixture(kit, List.of(new CollisionService.Obstacle(240, 240, 1, 120)),
                            Map.of("a_target", "slime|forest|300|300"));
                    int slot = kit.equals("water") ? 5 : kit.equals("wind") ? 3 : 4;
                    assertTrue(f.cast(slot, null));
                    f.advance(12);
                    assertEquals(218, f.world.skillPosition().x(), .01);
                    assertEquals(300, f.world.skillPosition().y(), .01);
                    assertEquals(1000, f.enemy(0).hp);
                    assertFalse(f.world.skillInDash());
                    assertTrue(f.world.skillReady());
                }));
    }

    @TestFactory Stream<DynamicTest> allThreeDashesPreserveSolidAllyBlocksAndValidEndpoints() {
        return Stream.of("water", "wind", "thunder").map(kit ->
                DynamicTest.dynamicTest(kit + " — aliado na rota", () -> {
                    var f = new Fixture(kit, List.of(), Map.of("a_target", "slime|forest|400|450"));
                    var ally = f.ally(230, 300);
                    int slot = kit.equals("water") ? 5 : kit.equals("wind") ? 3 : 4;
                    assertTrue(f.cast(slot, null));
                    f.advance(12);
                    assertEquals(206, f.world.skillPosition().x(), .01);
                    assertEquals(new Position(230, 300), ally.skillPosition());
                    assertFalse(f.world.skillInDash());
                    assertTrue(f.world.skillReady());
                }));
    }

    @Test void aDashUsesServerFacingAndEndsAtTheHistoricalMapBoundary() {
        var f = new Fixture("thunder", List.of(), Map.of("a_target", "slime|forest|140|200"));
        f.world.faceSyntheticDirection(0, -1);
        assertTrue(f.cast(4, null));
        f.advance(12);
        assertEquals(140, f.world.skillPosition().x(), .01);
        assertEquals(60, f.world.skillPosition().y(), .01);
        assertEquals(970, f.enemy(0).hp);
        assertTrue(f.world.skillReady());
        // Novo cenário junto à borda inferior: a cápsula não pode estender o contrato de coordenadas.
        var edge = new Fixture("thunder", List.of(), Map.of("a_target", "slime|forest|400|450"));
        assertTrue(edge.world.skillMove(new Position(140, 580)));
        edge.world.faceSyntheticDirection(0, 1);
        assertTrue(edge.cast(4, null));
        edge.advance(12);
        assertEquals(622, edge.world.skillPosition().y(), .01);
        assertTrue(edge.world.skillReady());
    }

    @Test void bodySlamKeepsVictimInsideTheMapAndAppliesOnlyOneImpact() {
        var f = new Fixture("martial", List.of(), Map.of("a_target", "slime|forest|19|300"));
        assertTrue(f.world.skillMove(new Position(60, 300)));
        assertTrue(f.cast(4, f.enemy(0)));
        f.advance(12);
        f.repeatTick();
        assertEquals(new Position(19, 300), f.enemy(0).getPosition());
        assertEquals(966, f.enemy(0).hp);
        assertEquals(1, f.encounter.events().stream().filter(event -> event.kind().equals("HIT")).count());
    }
}
