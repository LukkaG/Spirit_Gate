package com.projetok.multiplayer;

import com.projetok.studio.LiveRevision;
import com.projetok.studio.StudioExport;
import com.projetok.studio.WorldContent;
import com.projetok.studio.WorldDocument;
import java.net.InetAddress;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões da TASK-225 em servidor real e dois sockets TCP locais, sem autoridade no cliente. */
@Timeout(25)
class Task225ProjectileTcpTest {
    /** O mapa é preparado em memória; nenhum conteúdo publicado ou save é modificado. */
    private static LiveRevision scene(int enemyX, int enemyY, boolean wall) throws Exception {
        var maps = new TreeMap<>(WorldContent.baseline().bundle().maps());
        var forest = maps.get("forest");
        var elements = forest.elements().stream().map(element -> element.kind() == WorldDocument.Kind.ENEMY
                ? element.at(element.id(), enemyX, enemyY) : element).toList();
        forest = forest.withElements(elements).withHitboxes(wall
                ? List.of(new WorldDocument.Hitbox("task225_thin_wall", 240, 0, 1, 640, true, "collision"))
                : List.of());
        maps.put("forest", forest);
        return LiveRevision.prepare(0, "forest", maps, Task201CharacterSessionTest.EMPTY);
    }

    /** Conserva admissão, revisão e serialização reais, substituindo apenas a janela pelo renderer imediato. */
    private static LocalClient connect(LocalSimulationServer server, String name, int application) throws Exception {
        var selection = new SessionCharacterSelection(name, "reaper", "none", "none");
        var client = application == 7
                ? LocalClient.skillGame(InetAddress.getLoopbackAddress(), server.port(), selection,
                        false, false, null, Task201CharacterSessionTest.EMPTY)
                : LocalClient.gameplay(InetAddress.getLoopbackAddress(), server.port(), selection,
                        false, false, null, Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(prepared -> CompletableFuture.completedFuture(null));
        return client;
    }

    /** Falha com a exceção original do servidor, sem aguardar indefinidamente snapshots inexistentes. */
    private static void await(LocalSimulationServer server, Task201CharacterSessionTest.Check condition,
                              LocalClient... clients) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7);
        while (System.nanoTime() < deadline) {
            assertNull(server.failure(), "A thread autoritativa encerrou");
            for (var client : clients) {
                assertTrue(client.connected(), client.problem());
                client.measureLatency();
            }
            if (condition.get()) return;
            Thread.sleep(10);
        }
        assertTrue(condition.get(), "O estado TCP esperado não chegou no limite externo");
    }

    /** Aguarda a negociação e escolhe o domínio pela intenção de gameplay validada no servidor. */
    private static void prepare(LocalSimulationServer server, LocalClient caster, LocalClient observer) throws Exception {
        await(server, () -> ready(caster) && ready(observer), caster, observer);
        assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder"));
        await(server, () -> "thunder".equals(caster.playerSheet().progress().domain()), caster, observer);
    }

    private static boolean ready(LocalClient client) {
        return !client.revisions().blocked() && client.snapshot() != null && client.playerSheet() != null
                && (!client.skillApplication() || client.skills() != null);
    }

    private static Protocol.Actor enemy(LocalClient client) {
        return client.snapshot().actors().stream().filter(actor -> actor.id() > 1_000_000 && actor.id() < 2_000_000)
                .findFirst().orElseThrow();
    }

    private static List<SkillProtocol.Event> events(LocalClient client, int player, String skill) {
        return client.skills().events().stream().filter(event -> event.player() == player && event.skill().equals(skill)).toList();
    }

    private static List<Protocol.CombatEvent> hits(LocalClient client, int player) {
        return client.combatEvents().stream().filter(event -> event.player() == player && event.kind().equals("HIT")).toList();
    }

    private static boolean terminal(SkillProtocol.Event event) {
        return event.fromX() == event.x() && event.fromY() == event.y();
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 7})
    void movingTargetHasOneAuthoritativeHitAndObserversKeepTheirNegotiatedApplication(int observerApplication) throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        world.enableRevisions(scene(340, 370, false));
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world)) {
            server.start();
            try (var caster = connect(server, "Projetil Autoritativo", 7);
                 var observer = connect(server, "Projetil Observador", observerApplication)) {
                prepare(server, caster, observer);
                int target = enemy(caster).id();
                assertTrue(caster.selectTarget(target));
                long refusedAt = caster.snapshot().tick();
                assertTrue(caster.castSkill(1, target, 999_999, 0, 0));
                await(server, () -> caster.snapshot().tick() > refusedAt + 3, caster, observer);
                assertEquals(0, caster.skills().cooldowns().get(0), "Geração inventada não inicia recarga");
                assertTrue(events(caster, caster.id(), "thunder_1").isEmpty());
                int initialHp = enemy(caster).hp();
                var initialEnemy = enemy(caster);
                assertTrue(caster.castSkill(1, target, 0, 0, 0));
                await(server, () -> events(caster, caster.id(), "thunder_1").stream().anyMatch(Task225ProjectileTcpTest::terminal)
                        && !hits(caster, caster.id()).isEmpty() && !hits(observer, caster.id()).isEmpty(), caster, observer);
                var launch = events(caster, caster.id(), "thunder_1").stream().filter(event -> !terminal(event)).findFirst().orElseThrow();
                var impact = events(caster, caster.id(), "thunder_1").stream().filter(Task225ProjectileTcpTest::terminal).findFirst().orElseThrow();
                var hit = hits(caster, caster.id()).get(0);
                assertEquals(impact.tick(), hit.tick(), "O efeito terminal usa o tick do contato autoritativo");
                assertEquals(target, hit.enemy());
                assertEquals(1, hit.generation());
                assertTrue(impact.tick() > launch.tick(), "O projétil realmente percorre vários ticks");
                assertTrue(Math.hypot(initialEnemy.x() - enemy(caster).x(), initialEnemy.y() - enemy(caster).y()) > 1,
                        "O alvo deve se mover durante o cenário");
                assertEquals(initialHp - hit.amount(), enemy(caster).hp());
                assertEquals(hits(caster, caster.id()), hits(observer, caster.id()));
                if (observerApplication == 7) {
                    await(server, () -> events(observer, caster.id(), "thunder_1").contains(impact), caster, observer);
                    assertEquals(events(caster, caster.id(), "thunder_1"), events(observer, caster.id(), "thunder_1"));
                    var view = new NetworkSkillPresentation(() -> 1_000L);
                    view.actors(observer.snapshot());
                    view.update(observer.skills());
                    int active = view.activeCount();
                    view.update(observer.skills());
                    assertEquals(active, view.activeCount(), "Repetir o frame não duplica o efeito");
                } else {
                    assertFalse(observer.skillApplication());
                    assertNull(observer.skills(), "Aplicação 6 recebe combate, sem receber o frame 7");
                    assertFalse(observer.castSkill(1, target, 0, 0, 0));
                }
                long ended = impact.tick();
                await(server, () -> caster.snapshot().tick() > ended + 12 && observer.snapshot().tick() > ended + 12, caster, observer);
                assertEquals(1, hits(caster, caster.id()).size(), "Snapshots repetidos não aplicam novo dano");
                assertEquals(initialHp - hit.amount(), enemy(caster).hp());
                assertNull(server.failure());
                System.out.printf("TASK225 TCP app%d launch=%s impact=%s hit=%s enemyBefore=%s enemyAfter=%s%n",
                        observerApplication, launch, impact, hit, initialEnemy, enemy(caster));
            }
        }
    }

    @Test
    void frontalProjectileStopsAtOnePixelWallAndBothClientsSeeTheSameTerminal() throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        world.enableRevisions(scene(320, 300, true));
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world)) {
            server.start();
            try (var caster = connect(server, "Parede Lancador", 7); var observer = connect(server, "Parede Observador", 7)) {
                prepare(server, caster, observer);
                int hp = enemy(caster).hp();
                assertTrue(caster.castSkill(2, 0, 0, 0, 0));
                await(server, () -> events(caster, caster.id(), "thunder_2").stream().anyMatch(Task225ProjectileTcpTest::terminal)
                        && events(observer, caster.id(), "thunder_2").stream().anyMatch(Task225ProjectileTcpTest::terminal), caster, observer);
                var end = events(caster, caster.id(), "thunder_2").stream().filter(Task225ProjectileTcpTest::terminal).findFirst().orElseThrow();
                assertTrue(end.x() < 240, "A parede fina encerra a trajetória no lado do lançador");
                assertEquals(300, end.y(), .01);
                assertEquals(events(caster, caster.id(), "thunder_2"), events(observer, caster.id(), "thunder_2"));
                await(server, () -> caster.snapshot().tick() > end.tick() + 20, caster, observer);
                assertEquals(hp, enemy(caster).hp());
                assertEquals(hp, enemy(observer).hp());
                assertTrue(hits(caster, caster.id()).isEmpty());
                assertTrue(hits(observer, caster.id()).isEmpty());
                assertNull(server.failure());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"disconnect", "region"})
    void leavingDuringFlightCancelsDamageAndRemovesOldEvents(String departure) throws Exception {
        var world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        world.enableRevisions(scene(365, 300, false));
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world)) {
            server.start();
            try (var caster = connect(server, "Saida Lancador", 7); var observer = connect(server, "Saida Observador", 7)) {
                prepare(server, caster, observer);
                int id = caster.id(), hp = enemy(observer).hp(), target = enemy(caster).id();
                assertTrue(caster.selectTarget(target));
                assertTrue(caster.castSkill(1, target, 0, 0, 0));
                await(server, () -> !events(observer, id, "thunder_1").isEmpty(), caster, observer);
                var launch = events(observer, id, "thunder_1").get(0);
                assertFalse(terminal(launch));
                assertTrue(hits(observer, id).isEmpty(), "A saída deve ser exercitada durante o voo");
                var view = new NetworkSkillPresentation(() -> 1_000L);
                view.actors(observer.snapshot());
                view.update(observer.skills());
                assertTrue(view.activeCount() > 0);
                if (departure.equals("disconnect")) {
                    caster.close();
                    assertNull(caster.skills());
                } else {
                    assertTrue(caster.request(Protocol.WorldAction.TRAVEL, StudioExport.key("portal_", "forest/to_village")));
                }
                await(server, () -> observer.snapshot().actors().stream().noneMatch(actor -> actor.id() == id)
                        && events(observer, id, "thunder_1").isEmpty(), observer);
                view.actors(observer.snapshot());
                view.update(observer.skills());
                assertEquals(0, view.activeCount(), "O observador descarta o projétil de um ator que deixou sua região");
                await(server, () -> observer.snapshot().tick() > launch.tick() + 24, observer);
                assertEquals(hp, enemy(observer).hp(), "Um lançamento cancelado não pode causar dano tardio");
                assertTrue(hits(observer, id).isEmpty());
                assertTrue(events(observer, id, "thunder_1").isEmpty());
                if (departure.equals("region")) {
                    assertEquals("village", caster.region());
                    assertEquals(0, caster.selectedTarget());
                    assertTrue(caster.skills().events().isEmpty());
                }
                assertNull(server.failure());
            }
        }
    }
}
