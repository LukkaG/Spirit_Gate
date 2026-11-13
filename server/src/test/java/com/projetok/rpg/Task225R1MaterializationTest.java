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
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Investiga espera cumulativa e candidatos de saída somente em mundos descartáveis de teste. */
@Timeout(20)
public class Task225R1MaterializationTest {
    private static final Position ORIGIN = new Position(140, 300);

    /** Amostra imutável para dois renderers observadores, sem acrescentar dados ao protocolo. */
    public record ObserverTrace(List<RpgSkillEngine.Event> start, List<RpgSkillEngine.Event> waiting,
                                List<RpgSkillEngine.Event> terminal, long finalTick, long cutHits,
                                boolean stillIntangibleAt180) {
        public ObserverTrace { start = List.copyOf(start); waiting = List.copyOf(waiting); terminal = List.copyOf(terminal); }
    }

    /** Câmara artificial com apenas os apoios cobertos pelo aliado inserido durante a fase. */
    private static List<CollisionService.Obstacle> chamber() {
        return List.of(new CollisionService.Obstacle(0, 0, 110, 640),
                new CollisionService.Obstacle(170, 0, 790, 640),
                new CollisionService.Obstacle(110, 0, 60, 280),
                new CollisionService.Obstacle(110, 320, 60, 320));
    }

    /** O relógio do engine segue os mundos reais; não há timers nem alteração da implementação. */
    private static final class Fixture {
        final RpgSkillEngine engine = new RpgSkillEngine();
        final Map<Integer, RpgWorld> players = new LinkedHashMap<>();
        final RpgWorld world;
        final RpgSceneSet scenes;
        final SharedRpgEncounter encounter;
        RpgWorld blocker;
        long tick;

        Fixture(boolean blocked, boolean enemy) {
            var space = new CollisionService.Space(960, 640, blocked ? chamber() : List.of(), List.of());
            var definitions = enemy ? Map.of("r1_target", "slime|forest|144|300") : Map.<String, String>of();
            scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, ORIGIN),
                    "village", new RpgSceneSet.Geometry(space, ORIGIN)), definitions, Map.of(), Map.of(),
                    Map.of("escape", "forest|140|300|village|140|300"));
            world = new RpgWorld(CharacterIdentity.create("Fase R1", "reaper", "azure"), scenes);
            world.useNetworkFootprint();
            world.awardExperience(RpgProgression.builtin().threshold(15));
            assertTrue(world.chooseDomain("thunder"));
            encounter = new SharedRpgEncounter(Map.of("forest", space, "village", space), definitions,
                    SharedRpgEncounter.RewardPolicy.DISABLED);
            encounter.join(1, world);
            players.put(1, world);
            world.skillEnemies().forEach(victim -> { victim.hp = 1000; victim.skillRootUntil = 100_000; });
        }

        /** A ocupação é introduzida somente depois do cast, para reproduzir o risco condicional. */
        void castAndBlock() {
            cast();
            blocker = new RpgWorld(CharacterIdentity.create("Bloqueio R1", "human", "azure"), scenes);
            blocker.useNetworkFootprint();
            encounter.join(2, blocker);
            players.put(2, blocker);
        }

        /** O cast continua passando pelas validações reais de nível, energia, domínio e recarga. */
        void cast() {
            assertTrue(engine.cast(1, world, new RpgSkillEngine.Cast(5, 0, 0, 0, 0), players));
            assertTrue(world.skillIntangible());
        }

        /** Devolve somente o custo observado do engine; prepara mundo/encontro fora desse intervalo. */
        long advanceOne() {
            tick++;
            players.values().forEach(player -> player.tick(0, 0));
            encounter.step(tick);
            engine.clock(tick);
            long began = System.nanoTime();
            engine.tick(players);
            return System.nanoTime() - began;
        }

        /** Avanço finito protege a investigação independentemente do estado da pendência. */
        void advance(int count) { for (int index = 0; index < count; index++) advanceOne(); }

        /** Retira os corpos pela rotina real de encontro e registra eventual morte pelo ledger. */
        void releaseSupport() {
            if (blocker != null) { encounter.leave(blocker); players.remove(2); blocker = null; }
            for (var victim : world.skillEnemies()) if (victim.hp > 0)
                world.skillDamage(victim, victim.generation, victim.hp);
        }

        /** Conta os quatro cortes da técnica, distinguindo a morte usada para liberar o fixture. */
        long cuts() { return encounter.events().stream().filter(event -> event.kind().equals("HIT") && event.amount() == 14).count(); }
    }

    /** Área livre encerra aos 32 ticks; liberação após espera mantém apenas os quatro impactos. */
    @Test void normalMaterializationAndReleasedSupportKeepFourUniqueCuts() {
        var normal = new Fixture(false, false);
        normal.cast();
        normal.advance(31);
        assertTrue(normal.world.skillIntangible());
        normal.advance(1);
        assertFalse(normal.world.skillIntangible());
        assertEquals(2, normal.engine.events().size());

        var waiting = new Fixture(true, true);
        waiting.castAndBlock();
        waiting.advance(75);
        assertTrue(waiting.world.skillIntangible());
        assertEquals(4, waiting.cuts());
        waiting.releaseSupport();
        waiting.advance(1);
        assertFalse(waiting.world.skillIntangible());
        assertTrue(waiting.world.skillReady());
        assertEquals(ORIGIN, waiting.world.skillPosition());
        assertEquals(4, waiting.cuts());
        assertEquals(2, waiting.engine.events().size());
    }

    /** Mede sessenta tentativas sucessivas, inclusive dois mundos independentes em sequência. */
    @Test void cumulativeConsecutiveSearchesWriteRawEvidenceWithoutPerformanceGuarantees() throws IOException {
        var single = new Fixture(true, false);
        single.castAndBlock();
        single.advance(31);
        var samples = new ArrayList<Long>();
        for (int attempt = 0; attempt < 60; attempt++) {
            samples.add(single.advanceOne());
            assertTrue(single.world.skillIntangible());
        }
        var first = new Fixture(true, false);
        var second = new Fixture(true, false);
        first.castAndBlock(); second.castAndBlock();
        first.advance(31); second.advance(31);
        var paired = new ArrayList<Long>();
        for (int attempt = 0; attempt < 60; attempt++) {
            paired.add(first.advanceOne() + second.advanceOne());
            assertTrue(first.world.skillIntangible() && second.world.skillIntangible());
        }
        var folder = Files.createDirectories(Path.of("..").toAbsolutePath().normalize().resolve("docs/skills-preview-task225-r1"));
        String report = "A-04 R1: 60 tentativas consecutivas nos ticks32..91; mundos descartáveis em memória.\n"
                + "Custo medido: engine.tick; world.tick e encounter.step ficam fora do intervalo.\n"
                + "Cada busca completa pode examinar 37450 candidatos derivados dos laços atuais; 60 tentativas = 2247000 candidatos por mundo.\n"
                + "O ensaio de dois mundos é sequencial e independente: não representa dois participantes simultâneos numa sessão nem benchmark concorrente.\n"
                + stats("Um mundo", samples) + stats("Dois mundos sequenciais por rodada", paired)
                + "Sem limiar de aprovação por tempo; JIT/GC/agendamento afetam as amostras. Limite JUnit 20 s/teste e externo Maven pelo executor.\n";
        Files.writeString(folder.resolve("a04-consecutive-searches.txt"), report);
        System.out.print(report);
    }

    /** Resume total cumulativo e extremos sem converter amostras locais em promessa de carga. */
    private static String stats(String label, List<Long> samples) {
        var sorted = samples.stream().sorted().toList();
        return String.format(Locale.ROOT, "%s: tentativas=%d total=%.3fms min=%.3fms mediana=%.3fms max=%.3fms ns=%s%n",
                label, samples.size(), samples.stream().mapToLong(Long::longValue).sum() / 1e6,
                sorted.get(0) / 1e6, sorted.get(sorted.size() / 2) / 1e6, sorted.get(sorted.size() - 1) / 1e6, samples);
    }

    /** Candidato exclusivamente de teste: aos 92 ticks remove engine, encontro e mapa de participantes. */
    @Test void isolatedNinetyTwoTickCandidateRemovesParticipationWithoutChangingRuntimePolicy() {
        var f = new Fixture(true, true);
        f.castAndBlock();
        f.advance(91);
        assertTrue(f.world.skillIntangible());
        assertEquals(4, f.cuts());
        f.advance(1);
        assertTrue(f.world.skillIntangible(), "A produção atual continua esperando no tick92");
        assertEquals(92, f.tick);

        // Modelo candidato externo ao engine; não fecha socket nem valida política de produto.
        f.engine.remove(1);
        f.encounter.leave(f.world);
        f.players.remove(1);
        assertFalse(f.world.skillIntangible());
        assertFalse(f.world.skillInDash());
        assertTrue(f.engine.events().isEmpty());
        assertEquals(4, f.cuts());
        assertEquals(List.of(0, 0, 0, 0, 0), f.engine.state(1).cooldowns());
        f.advance(10);
        assertEquals(4, f.cuts());

        var next = new Fixture(false, false);
        assertNotEquals(f.world.networkState().identity().id(), next.world.networkState().identity().id());
        assertTrue(next.world.syntheticPositionFree(List.of()));
        assertTrue(next.world.skillReady());
        assertFalse(next.world.skillIntangible());
        assertEquals(List.of(0, 0, 0, 0, 0), next.engine.state(1).cooldowns());
    }

    /** Remoção, perda e troca de sessão não dependem de encontrar um apoio na câmara bloqueada. */
    @TestFactory Stream<DynamicTest> lifecycleCleanupDuringBlockedWaitingDoesNotTransferPhase() {
        return Stream.of("remove", "clear", "perda", "substituição", "região").map(action ->
                DynamicTest.dynamicTest(action, () -> {
                    var f = new Fixture(true, false);
                    f.castAndBlock();
                    f.advance(60);
                    assertTrue(f.world.skillIntangible());
                    switch (action) {
                        case "remove" -> f.engine.remove(1);
                        case "clear" -> f.engine.clear();
                        case "perda" -> { f.players.remove(1); f.engine.tick(f.players); }
                        case "substituição" -> {
                            var next = new Fixture(false, false);
                            f.players.put(1, next.world); f.engine.tick(f.players);
                            assertTrue(next.world.skillReady());
                            assertFalse(next.world.skillIntangible());
                        }
                        case "região" -> { assertTrue(f.world.travel("escape")); f.engine.tick(f.players); }
                        default -> fail(action);
                    }
                    assertFalse(f.world.skillIntangible());
                    assertFalse(f.world.skillInDash());
                    assertTrue(f.engine.events().isEmpty());
                    assertEquals(List.of(0, 0, 0, 0, 0), f.engine.state(1).cooldowns());
                }));
    }

    /** Dano comum não mata durante a imunidade; após remoção a morte volta ao fluxo normal do mundo. */
    @Test void fatalDamageIsIgnoredDuringPhaseAndAcceptedAfterLifecycleCleanup() {
        var f = new Fixture(true, false);
        f.castAndBlock(); f.advance(60);
        int before = f.world.networkState().hp();
        f.world.receiveDamage(10_000);
        assertEquals(before, f.world.networkState().hp());
        f.engine.remove(1);
        f.world.receiveDamage(10_000);
        assertEquals(0, f.world.networkState().hp());
        assertFalse(f.world.skillIntangible());
        assertTrue(f.engine.events().isEmpty());
    }

    /** Produz eventos reais finitos para observadores independentes, sem campo novo no contrato. */
    public static ObserverTrace observerTrace() {
        var f = new Fixture(true, true);
        f.castAndBlock();
        var start = f.engine.events();
        f.advance(180);
        var waiting = f.engine.events();
        boolean intangible = f.world.skillIntangible();
        f.releaseSupport(); f.advance(1);
        return new ObserverTrace(start, waiting, f.engine.events(), f.tick, f.cuts(), intangible);
    }
}
