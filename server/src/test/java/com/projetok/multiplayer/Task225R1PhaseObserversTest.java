package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillEngine;
import com.projetok.rpg.Task225R1MaterializationTest;
import com.projetok.studio.LiveRevision;
import com.projetok.studio.WorldContent;
import com.projetok.studio.WorldDocument;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Observadores independentes e TCP real conservam o contrato atual da fase e de reconexão. */
@Timeout(25)
class Task225R1PhaseObserversTest {
    private static final String HASH = "a".repeat(64);

    /** A mesma serialização existente entrega eventos reais a duas apresentações independentes. */
    @Test void twoSerializedObserversExposeMissingTerminalAndExistingFiveSecondFuse() throws Exception {
        var trace = Task225R1MaterializationTest.observerTrace();
        var clock = new AtomicLong();
        var first = new NetworkSkillPresentation(clock::get);
        var second = new NetworkSkillPresentation(clock::get);
        var actors = new Protocol.State(0, List.of(new Protocol.Actor(1, "Fase R1", 140, 300, 100, 100, 1000, 0, false)));
        var start = frame(0, trace.start());
        for (var view : List.of(first, second)) {
            view.update(roundTrip(start)); view.actors(actors);
            assertTrue(view.hidden(1));
        }
        clock.set(6000);
        for (var view : List.of(first, second)) {
            view.update(roundTrip(frame(180, trace.waiting())));
            assertFalse(view.hidden(1));
        }
        assertTrue(trace.stillIntangibleAt180(), "O servidor ainda espera quando ambos os observadores encerram o fusível");
        assertEquals(4, trace.cutHits());
        clock.set(trace.finalTick() * 1000 / 30);
        var terminal = frame(trace.finalTick(), trace.terminal());
        first.update(roundTrip(terminal));
        second.update(roundTrip(frame(180, trace.waiting()))); // Perda deliberada somente no observador de teste.
        assertEquals(1, first.activeCount());
        assertEquals(0, second.activeCount());
        first.update(roundTrip(terminal));
        assertEquals(1, first.activeCount(), "Repetição não duplica a apresentação dos quatro cortes");
        first.update(null); second.region("village");
        assertEquals(0, first.activeCount()); assertEquals(0, second.activeCount());
    }

    /** O mapa é descartável e mantém o piso de HP da produção e o transporte sem extensões. */
    private static LiveRevision scene(int enemyX) throws Exception {
        var maps = new TreeMap<>(WorldContent.baseline().bundle().maps());
        var forest = maps.get("forest");
        maps.put("forest", forest.withHitboxes(List.of()).withElements(forest.elements().stream()
                .map(element -> element.kind() == WorldDocument.Kind.ENEMY ? element.at(element.id(), enemyX, 300) : element).toList()));
        return LiveRevision.prepare(0, "forest", maps, Task201CharacterSessionTest.EMPTY);
    }

    /** A janela é substituída apenas por renderer imediato; protocolo, socket e coordenador são reais. */
    private static LocalClient connect(LocalSimulationServer server, String name) throws Exception {
        var client = LocalClient.skillGame(InetAddress.getLoopbackAddress(), server.port(),
                new SessionCharacterSelection(name, "reaper", "none", "none"), false, false, null,
                Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(prepared -> CompletableFuture.completedFuture(null));
        return client;
    }

    /** Aguarda estados públicos e escolhe o domínio por comando legítimo. */
    private static void prepare(LocalClient caster, LocalClient observer) throws Exception {
        Task201CharacterSessionTest.ready(caster, observer);
        Task201CharacterSessionTest.waitFor(() -> caster.skills() != null && observer.skills() != null
                && caster.playerSheet() != null, caster, observer);
        assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder"));
        Task201CharacterSessionTest.waitFor(() -> caster.playerSheet().progress().domain().equals("thunder"), caster, observer);
    }

    /** Dois sockets observam início e término iguais e exatamente quatro impactos do servidor. */
    @Test void realCasterAndObserverReceiveNormalMaterializationAndFourUniqueImpacts() throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        world.enableRevisions(scene(185));
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world)) {
            server.start();
            try (var caster = connect(server, "Fase Lancador"); var observer = connect(server, "Fase Observador")) {
                prepare(caster, observer);
                int hp = enemy(caster).hp();
                assertEquals(10_000, hp);
                assertTrue(caster.castSkill(5, 0, 0, 0, 0));
                Task201CharacterSessionTest.waitFor(() -> phaseEvents(caster, caster.id()).stream().anyMatch(Task225R1PhaseObserversTest::terminal)
                        && phaseEvents(observer, caster.id()).stream().anyMatch(Task225R1PhaseObserversTest::terminal), caster, observer);
                assertEquals(phaseEvents(caster, caster.id()), phaseEvents(observer, caster.id()));
                assertEquals(2, phaseEvents(caster, caster.id()).size());
                var hits = caster.combatEvents().stream().filter(event -> event.player() == caster.id() && event.kind().equals("HIT")).toList();
                assertEquals(4, hits.size());
                assertEquals(hits, observer.combatEvents().stream().filter(event -> event.player() == caster.id() && event.kind().equals("HIT")).toList());
                assertEquals(hp - hits.stream().mapToInt(Protocol.CombatEvent::amount).sum(), enemy(caster).hp());
                assertTrue(caster.skills().cooldowns().get(4) > 0);
                var firstView = new NetworkSkillPresentation(() -> 0L);
                var secondView = new NetworkSkillPresentation(() -> 0L);
                for (var view : List.of(firstView, secondView)) {
                    view.update(observer.skills()); view.actors(observer.snapshot());
                    assertFalse(view.hidden(caster.id()));
                }
                assertEquals(firstView.activeCount(), secondView.activeCount());
                assertNull(server.failure());
            }
        }
    }

    /** Reconexão durante a fase limpa o corpo anterior e cria outra sessão sem recarga herdada. */
    @Test void reconnectDuringPhaseRemovesOldCasterAndAdmitsANewValidWorld() throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        world.enableRevisions(scene(340));
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world)) {
            server.start();
            try (var caster = connect(server, "Fase Reconexao"); var observer = connect(server, "Fase Testemunha")) {
                prepare(caster, observer);
                int old = caster.id(), before = enemy(observer).hp();
                assertTrue(caster.castSkill(5, 0, 0, 0, 0));
                Task201CharacterSessionTest.waitFor(() -> !phaseEvents(observer, old).isEmpty(), caster, observer);
                assertTrue(phaseEvents(observer, old).stream().noneMatch(Task225R1PhaseObserversTest::terminal));
                var view = new NetworkSkillPresentation(() -> 0L);
                view.update(observer.skills()); view.actors(observer.snapshot());
                assertTrue(view.hidden(old));
                caster.close();
                Task201CharacterSessionTest.waitFor(() -> server.players() == 1
                        && observer.snapshot().actors().stream().noneMatch(actor -> actor.id() == old)
                        && phaseEvents(observer, old).isEmpty(), observer);
                view.update(observer.skills()); view.actors(observer.snapshot());
                assertFalse(view.hidden(old));
                try (var next = caster.reconnect()) {
                    next.revisions().renderer(prepared -> CompletableFuture.completedFuture(null));
                    Task201CharacterSessionTest.ready(next, observer);
                    Task201CharacterSessionTest.waitFor(() -> next.skills() != null && next.playerSheet() != null
                            && observer.snapshot().actors().stream().anyMatch(actor -> actor.id() == next.id()), next, observer);
                    assertNotEquals(old, next.id());
                    assertEquals(List.of(0, 0, 0, 0, 0), next.skills().cooldowns());
                    assertEquals("none", next.playerSheet().progress().domain());
                    assertTrue(phaseEvents(observer, old).isEmpty());
                    assertEquals(before, enemy(observer).hp(), "Fase cancelada antes do pulso não causa dano tardio");
                    var actor = next.snapshot().actors().stream().filter(value -> value.id() == next.id()).findFirst().orElseThrow();
                    assertTrue(actor.x() >= 18 && actor.x() <= 942 && actor.y() >= 18 && actor.y() <= 622);
                    assertTrue(actor.hp() > 0);
                }
                assertNull(server.failure());
            }
        }
    }

    /** Copia o DTO usando os bytes vigentes, sem adaptar campos ou inferir estado inexistente. */
    private static SkillProtocol.State roundTrip(SkillProtocol.State state) throws Exception {
        var bytes = new ByteArrayOutputStream(); Protocol.write(bytes, state);
        return (SkillProtocol.State) Protocol.read(new ByteArrayInputStream(bytes.toByteArray()));
    }

    /** Converte eventos do engine ao DTO existente, como faz o snapshot de produção. */
    private static SkillProtocol.State frame(long tick, List<RpgSkillEngine.Event> events) {
        return new SkillProtocol.State(HASH, tick, events.stream().map(event -> new SkillProtocol.Event(event.sequence(), event.tick(),
                event.player(), event.skill(), event.fromX(), event.fromY(), event.x(), event.y())).toList(), List.of(0, 0, 0, 0, 0), 0, false);
    }

    /** Retém somente a fase do lançador investigado, sem depender da ordem de outros eventos. */
    private static List<SkillProtocol.Event> phaseEvents(LocalClient client, int player) {
        return client.skills().events().stream().filter(event -> event.player() == player && event.skill().equals("thunder_5")).toList();
    }

    /** O evento terminal tem origem e destino iguais no contrato existente. */
    private static boolean terminal(SkillProtocol.Event event) { return event.fromX() == event.x() && event.fromY() == event.y(); }

    /** Localiza o mob pelo espaço de IDs do protocolo, sem atingir um jogador. */
    private static Protocol.Actor enemy(LocalClient client) {
        return client.snapshot().actors().stream().filter(actor -> actor.id() > 1_000_000 && actor.id() < 2_000_000).findFirst().orElseThrow();
    }
}
