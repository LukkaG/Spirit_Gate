package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Investiga A-04 em mundos descartáveis, com relógio e execução limitados pelo teste. */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class Task225MaterializationTest {
    private static final Position ORIGIN = new Position(140, 300);

    /** Câmara artificial: todos os apoios válidos ficam ao alcance de um aliado na origem. */
    private static List<CollisionService.Obstacle> chamber() {
        return List.of(new CollisionService.Obstacle(0, 0, 110, 640),
                new CollisionService.Obstacle(170, 0, 790, 640),
                new CollisionService.Obstacle(110, 0, 60, 280),
                new CollisionService.Obstacle(110, 320, 60, 320));
    }

    /** Fixture sem disco, timers ou transporte; preserva o perfil físico real da sessão. */
    private static final class Fixture {
        final RpgSkillEngine engine = new RpgSkillEngine();
        final Map<Integer, RpgWorld> players = new LinkedHashMap<>();
        final SharedRpgEncounter encounter;
        final RpgWorld world;
        final RpgSceneSet scenes;
        long tick;

        Fixture(List<CollisionService.Obstacle> obstacles, Position spawn, boolean enemy) {
            var space = new CollisionService.Space(960, 640, obstacles, List.of());
            var definitions = enemy ? Map.of("a_target", "slime|forest|144|300") : Map.<String, String>of();
            scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, spawn),
                    "village", new RpgSceneSet.Geometry(space, spawn)), definitions, Map.of(), Map.of(),
                    Map.of("escape", "forest|140|300|village|140|300"));
            world = new RpgWorld(CharacterIdentity.create("Fase", "reaper", "azure"), scenes);
            world.useNetworkFootprint();
            world.awardExperience(RpgProgression.builtin().threshold(15));
            assertTrue(world.chooseDomain("thunder"));
            encounter = new SharedRpgEncounter(Map.of("forest", space, "village", space),
                    definitions, SharedRpgEncounter.RewardPolicy.DISABLED);
            encounter.join(1, world);
            players.put(1, world);
            world.skillEnemies().forEach(target -> { target.hp = 1000; target.skillRootUntil = 100_000; });
        }

        /** Introduz ocupação durante a intangibilidade, sem modificar geometria em produção. */
        RpgWorld occupyOrigin() {
            var ally = new RpgWorld(CharacterIdentity.create("Apoio", "human", "azure"), scenes);
            ally.useNetworkFootprint();
            encounter.join(2, ally);
            players.put(2, ally);
            return ally;
        }

        /** Usa a mesma ordem de atualização do executor de sessão; nunca espera tempo de parede. */
        void advance(int count) {
            for (int i = 0; i < count; i++) {
                tick++;
                players.values().forEach(player -> player.tick(0, 0));
                encounter.step(tick);
                engine.clock(tick);
                engine.tick(players);
            }
        }

        /** O especial é SELF e não precisa inventar um alvo para iniciar a fase. */
        void cast() {
            assertTrue(engine.cast(1, world, new RpgSkillEngine.Cast(5, 0, 0, 0, 0), players));
            assertTrue(world.skillIntangible());
        }
    }

    /** Cenários normais continuam encerrando a fase na duração já prevista. */
    @TestFactory Stream<DynamicTest> freeSupportNearWallsBordersAndObstaclesMaterializesNormally() {
        record Scenario(String name, Position spawn, List<CollisionService.Obstacle> obstacles) { }
        return Stream.of(new Scenario("área livre", ORIGIN, List.of()),
                new Scenario("parede próxima", ORIGIN, List.of(new CollisionService.Obstacle(165, 240, 1, 120))),
                new Scenario("borda", new Position(18, 300), List.of()),
                new Scenario("obstáculos próximos", ORIGIN, List.of(new CollisionService.Obstacle(170, 260, 20, 80),
                        new CollisionService.Obstacle(90, 260, 20, 80))))
                .map(scenario -> DynamicTest.dynamicTest(scenario.name(), () -> {
                    var f = new Fixture(scenario.obstacles(), scenario.spawn(), false);
                    f.cast();
                    f.advance(31);
                    assertTrue(f.world.skillIntangible());
                    f.advance(1);
                    assertFalse(f.world.skillIntangible());
                    assertFalse(f.world.skillInDash());
                    assertTrue(f.world.skillReady());
                    assertEquals(scenario.spawn(), f.world.skillPosition());
                    assertTrue(f.world.syntheticPositionFree(List.of()));
                    assertEquals(2, f.engine.events().size());
                }));
    }

    /** Um aliado na origem provoca busca local sem empurrão, passagem de parede ou apoio inválido. */
    @Test void allyOnTheOriginMaterializesCasterAtAnotherValidSupport() {
        var f = new Fixture(List.of(new CollisionService.Obstacle(165, 240, 1, 120)), ORIGIN, false);
        f.cast();
        var ally = f.occupyOrigin();
        f.advance(32);
        assertFalse(f.world.skillIntangible());
        assertNotEquals(ORIGIN, f.world.skillPosition());
        assertEquals(ORIGIN, ally.skillPosition());
        assertTrue(f.world.skillPosition().x() <= 143.001f, "Não atravessar a parede para materializar");
        assertTrue(Math.abs(f.world.skillPosition().x() - ally.skillPosition().x()) >= 24
                || Math.abs(f.world.skillPosition().y() - ally.skillPosition().y()) >= 24);
        assertTrue(f.world.skillReappear());
    }

    /** Reproduz a espera sem limite por seis segundos simulados, com quatro danos somente uma vez. */
    @Test void noArtificialSupportKeepsImmunityBeyondVisualFuseButReentersWhenSpaceReturns() {
        var f = new Fixture(chamber(), ORIGIN, true);
        f.cast();
        var ally = f.occupyOrigin();
        int hp = f.world.networkState().hp();
        f.advance(180);
        assertTrue(f.world.skillIntangible(), "A-04: o servidor continua intangível após seis segundos");
        assertTrue(f.world.skillInDash());
        assertFalse(f.world.skillReady());
        assertEquals("Aguardando espaço livre para reaparecer.", f.world.networkMessage());
        f.world.receiveDamage(100);
        assertEquals(hp, f.world.networkState().hp());
        assertEquals(944, f.world.skillEnemies().get(0).hp, "Quatro cortes de 14, sem repetição na espera");
        assertEquals(4, f.encounter.events().stream().filter(event -> event.kind().equals("HIT")).count());
        assertEquals(1, f.engine.events().size(), "Ainda não há evento terminal autoritativo");

        // A fixture libera ocupantes; não altera a regra de materialização nem o mapa em execução.
        f.encounter.leave(ally);
        f.players.remove(2);
        var victim = f.world.skillEnemies().get(0);
        assertEquals(944, f.world.skillDamage(victim, victim.generation, victim.hp));
        f.advance(1);
        assertFalse(f.world.skillIntangible());
        assertTrue(f.world.skillReady());
        assertEquals(ORIGIN, f.world.skillPosition());
        assertEquals(2, f.engine.events().size());
        f.world.receiveDamage(1);
        assertEquals(hp - 1, f.world.networkState().hp());
    }

    /** Mede a busca completa do pior caso sintético sem impor limiar de desempenho dependente da máquina. */
    @Test void measuresCompleteSearchAndWritesBoundedReproductionEvidence() throws IOException {
        var blocked = new Fixture(chamber(), ORIGIN, false);
        blocked.cast();
        blocked.occupyOrigin();
        var free = new Fixture(List.of(), ORIGIN, false);
        for (int warmup = 0; warmup < 3; warmup++) assertFalse(blocked.world.skillReappear());
        var blockedNanos = new ArrayList<Long>();
        var freeNanos = new ArrayList<Long>();
        for (int sample = 0; sample < 15; sample++) {
            long before = System.nanoTime();
            assertFalse(blocked.world.skillReappear());
            blockedNanos.add(System.nanoTime() - before);
            before = System.nanoTime();
            assertTrue(free.world.skillReappear());
            freeNanos.add(System.nanoTime() - before);
        }
        var folder = Files.createDirectories(Path.of("..").toAbsolutePath().normalize().resolve("docs/skills-preview-task225"));
        String evidence = "A-04: mundo sintético descartável 960x640; perfil original-ground v1, raio 12.\n"
                + "Câmara bruta x=110..170/y=280..320; aliado no apoio 140,300.\n"
                + "Sem apoio livre: origem + 1024 sondas radiais + grade 235x155 (36425) = 37450 pontos por tentativa.\n"
                + "A grade depende da implementação atual; contagem derivada dos limites/loops, não instrumentada.\n"
                + "3 aquecimentos + 15 amostras; tempos incluem chamada/assert e não são benchmark de carga.\n"
                + stats("Sem apoio", blockedNanos) + stats("Origem livre", freeNanos)
                + "Limite externo JUnit: 20 s por teste. Sem alteração de imunidade/duração/materialização.\n";
        Files.writeString(folder.resolve("a04-measurements.txt"), evidence);
        System.out.print(evidence);
    }

    /** Resume amostras observadas, preservando os valores brutos para conferência. */
    private static String stats(String label, List<Long> nanos) {
        var sorted = nanos.stream().sorted().toList();
        return String.format(Locale.ROOT, "%s: min=%.3f ms; mediana=%.3f ms; max=%.3f ms; ns=%s%n", label,
                sorted.get(0) / 1_000_000d, sorted.get(sorted.size() / 2) / 1_000_000d,
                sorted.get(sorted.size() - 1) / 1_000_000d, nanos);
    }

    /** Perda, substituição e cancelamento de sessão limpam os flags no mundo descartado. */
    @TestFactory Stream<DynamicTest> lifecycleCleanupReleasesOldWorldAndDoesNotTransferPhase() {
        return Stream.of("remove", "clear", "perda", "substituição", "região")
                .map(action -> DynamicTest.dynamicTest(action, () -> {
                    var f = new Fixture(List.of(), ORIGIN, false);
                    f.cast();
                    f.advance(5);
                    switch (action) {
                        case "remove" -> f.engine.remove(1);
                        case "clear" -> f.engine.clear();
                        case "perda" -> { f.players.remove(1); f.engine.tick(f.players); }
                        case "substituição" -> {
                            var next = new Fixture(List.of(), ORIGIN, false);
                            f.players.put(1, next.world);
                            f.engine.tick(f.players);
                            assertFalse(next.world.skillIntangible());
                            assertTrue(next.world.skillReady());
                        }
                        case "região" -> { assertTrue(f.world.travel("escape")); f.engine.tick(f.players); }
                        default -> fail(action);
                    }
                    assertFalse(f.world.skillIntangible());
                    assertFalse(f.world.skillInDash());
                    assertEquals(List.of(), f.engine.events());
                    assertEquals(List.of(0, 0, 0, 0, 0), f.engine.state(1).cooldowns());
                    f.world.receiveDamage(1);
                    assertTrue(f.world.networkState().hp() < f.world.maxHp());
                }));
    }
}
