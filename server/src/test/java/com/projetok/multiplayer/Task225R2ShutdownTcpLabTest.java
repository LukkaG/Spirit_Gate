package com.projetok.multiplayer;

import com.projetok.rpg.RpgWorld;
import com.projetok.studio.*;
import com.projetok.domain.movement.CollisionService;
import java.lang.reflect.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Dois clientes TCP reais; adaptador interno existe apenas no classpath de testes. */
@Timeout(30)
class Task225R2ShutdownTcpLabTest {
    /** Vincula o pedido ao objeto de mundo, região, revisão e sequência inicial da execução real. */
    private record End(int id, RpgWorld world, String hash, String region, long execution) { }

    /** Decorador de laboratório aciona a saída existente após step, antes dos snapshots privados. */
    private static final class Lab implements InvocationHandler {
        final RpgSessionWorld world = new RpgSessionWorld(List.of("reaper", "reaper"), null, true, 20);
        final Queue<Runnable> jobs = new ConcurrentLinkedQueue<>();
        final Set<Integer> ended = ConcurrentHashMap.newKeySet();
        final AtomicInteger logicalRemovals = new AtomicInteger(), departureCallbacks = new AtomicInteger();
        final AtomicInteger missingPrivateSnapshots = new AtomicInteger();
        volatile String ownerThread;
        final SessionSimulation simulation;

        Lab() throws Exception {
            var maps = new TreeMap<>(WorldContent.baseline().bundle().maps());
            var forest = maps.get("forest");
            maps.put("forest", forest.withHitboxes(List.of()).withElements(forest.elements().stream()
                    .map(e -> e.kind() == WorldDocument.Kind.ENEMY ? e.at(e.id(), 750, 300) : e).toList()));
            world.enableRevisions(LiveRevision.prepare(0, "forest", maps, Task201CharacterSessionTest.EMPTY));
            simulation = (SessionSimulation) Proxy.newProxyInstance(SessionSimulation.class.getClassLoader(),
                    new Class<?>[]{SessionSimulation.class}, this);
        }

        /** Reflexão é leitura/injeção restrita ao teste; não cria API administrativa no runtime. */
        static Object field(Object object, String name) {
            try { var f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }

        /** Consulta somente na thread proprietária por meio da fila interna do ensaio. */
        RpgWorld player(int id) {
            var session = ((Map<?, ?>) field(world, "sessions")).get(id);
            return session == null ? null : (RpgWorld) field(session, "world");
        }

        /** Mantém o mesmo executor de mundo do servidor e limita toda espera do chamador. */
        <T> T owner(Supplier<T> operation) throws Exception {
            var result = new CompletableFuture<T>();
            jobs.add(() -> {
                try { result.complete(operation.get()); }
                catch (Throwable e) { result.completeExceptionally(e); }
            });
            return result.get(5, TimeUnit.SECONDS);
        }

        /** Captura a execução efetiva sem inventar identidade pelo nome ou endereço do cliente. */
        End token(int id) {
            var event = world.skillsSnapshotFor(id).events().stream()
                    .filter(e -> e.player() == id && e.skill().equals("thunder_5")).findFirst().orElseThrow();
            return new End(id, player(id), world.contentHash(), world.region(id), event.sequence());
        }

        /** O pedido persiste após leave até o transporte o consultar; duplicatas são inofensivas. */
        boolean retire(End end) {
            assertEquals("coop-world", Thread.currentThread().getName());
            if (player(end.id()) != end.world() || !world.contentHash().equals(end.hash())
                    || !world.region(end.id()).equals(end.region())
                    || world.skillsSnapshotFor(end.id()).events().stream().noneMatch(e -> e.player() == end.id()
                    && e.sequence() == end.execution())) return false;
            world.leave(end.id()); ended.add(end.id()); logicalRemovals.incrementAndGet();
            assertFalse(end.world().skillIntangible()); assertFalse(end.world().skillInDash());
            return true;
        }

        /** Delega contratos reais e detecta acesso privado depois da remoção, sem mascarar exceções. */
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("disconnectRequested") && ended.contains((Integer) args[0])) return true;
            if (name.equals("skillsSnapshotFor") || name.equals("gameplaySnapshotFor")) {
                if (player((Integer) args[0]) == null) missingPrivateSnapshots.incrementAndGet();
            }
            if (name.equals("leave")) {
                ownerThread = Thread.currentThread().getName(); departureCallbacks.incrementAndGet();
                if (player((Integer) args[0]) != null) logicalRemovals.incrementAndGet();
            }
            try {
                Object result = method.invoke(world, args);
                if (name.equals("step")) {
                    ownerThread = Thread.currentThread().getName();
                    for (int i = 0; i < 32; i++) { var job = jobs.poll(); if (job == null) break; job.run(); }
                }
                return result;
            } catch (InvocationTargetException e) { throw e.getCause(); }
        }

        /** Falha controlada apenas na validação de spawn do coordenador, com restauração imediata. */
        Object blockAdmission() {
            Object previous = field(world, "geometry");
            geometry(new CollisionService.Space(960, 640,
                    List.of(new CollisionService.Obstacle(0, 0, 960, 640)), List.of()));
            return previous;
        }

        /** Não altera mapas/arquivos nem os mundos já admitidos; exclusiva do laboratório. */
        void geometry(Object value) {
            try { var f = RpgSessionWorld.class.getDeclaredField("geometry"); f.setAccessible(true); f.set(world, value); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
    }

    /** Somente a renderização é imediata; handshake, transporte e sincronização são reais. */
    private static LocalClient connect(LocalSimulationServer server, String name, int application) throws Exception {
        var selection = new SessionCharacterSelection(name, "reaper", "none", "none");
        var client = application == 7
                ? LocalClient.skillGame(InetAddress.getLoopbackAddress(), server.port(), selection,
                    false, false, null, Task201CharacterSessionTest.EMPTY)
                : LocalClient.gameplay(InetAddress.getLoopbackAddress(), server.port(), selection,
                    false, false, null, Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(p -> CompletableFuture.completedFuture(null));
        return client;
    }

    /** Confirma pedido, owner, peer, observador, fila antiga, spawn e identidade na mesma sessão real. */
    @ParameterizedTest @ValueSource(ints = {6, 7})
    void coordinatedExitAndReadmissionUseExistingTransport(int observerApplication) throws Exception {
        var lab = new Lab();
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, lab.simulation)) {
            server.start();
            try (var caster = connect(server, "Saida R2", 7);
                 var observer = connect(server, "Observador R2", observerApplication)) {
                Task201CharacterSessionTest.ready(caster, observer);
                Task201CharacterSessionTest.waitFor(() -> caster.skills() != null && observer.playerSheet() != null, caster, observer);
                assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder"));
                if (observerApplication == 7) assertTrue(observer.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder"));
                Task201CharacterSessionTest.waitFor(() -> caster.playerSheet().progress().domain().equals("thunder")
                        && (observerApplication != 7 || observer.playerSheet().progress().domain().equals("thunder")), caster, observer);
                assertTrue(caster.castSkill(5, 0, 0, 0, 0));
                if (observerApplication == 7) assertTrue(observer.castSkill(5, 0, 0, 0, 0));
                Task201CharacterSessionTest.waitFor(() -> caster.skills().events().stream().anyMatch(e -> e.player() == caster.id())
                        && (observerApplication != 7 || observer.skills().events().stream().anyMatch(e -> e.player() == observer.id())), caster, observer);
                int old = caster.id();
                var view = new NetworkSkillPresentation(() -> 0L);
                view.update(caster.skills()); view.actors(caster.snapshot()); assertTrue(view.hidden(old));
                var oldIdentity = lab.owner(() -> lab.player(old).networkState().identity().id());
                var observerIdentity = lab.owner(() -> lab.player(observer.id()).networkState().identity().id());
                var token = lab.owner(() -> lab.token(old));
                var queuedCommands = new CompletableFuture<Boolean>();
                assertTrue(lab.owner(() -> {
                    assertFalse(lab.retire(new End(observer.id(), token.world(), token.hash(), token.region(), token.execution())));
                    assertFalse(lab.retire(new End(old, token.world(), "b".repeat(64), token.region(), token.execution())));
                    assertFalse(lab.retire(new End(old, token.world(), token.hash(), token.region(), token.execution() + 100)));
                    // A fila real será drenada no próximo tick, depois de departures; foi preenchida antes de leave.
                    @SuppressWarnings("unchecked") var queue = (Queue<Runnable>) Lab.field(server, "commands");
                    assertTrue(queue.offer(() -> {
                        try {
                            assertFalse(lab.world.accept(old, new Protocol.Intent(99999, Protocol.Action.MOVE, 1, 0)));
                            assertFalse(lab.world.skill(old, new SkillProtocol.Request(99999, 5, 0, 0, 0, 0)));
                            assertFalse(lab.world.gameplay(old, new GameplayProtocol.Request(99999,
                                    GameplayProtocol.Operation.CHOOSE_DOMAIN, "fire")));
                            queuedCommands.complete(true);
                        } catch (Throwable e) { queuedCommands.completeExceptionally(e); }
                    }));
                    assertTrue(lab.retire(token)); assertFalse(lab.retire(token));
                    assertEquals(observerIdentity, lab.player(observer.id()).networkState().identity().id());
                    if (observerApplication == 7) {
                        assertTrue(lab.player(observer.id()).skillIntangible());
                        assertTrue(lab.world.skillsSnapshotFor(observer.id()).events().stream().anyMatch(e -> e.player() == observer.id()));
                    }
                    return true;
                }));
                assertTrue(queuedCommands.get(5, TimeUnit.SECONDS));
                Task201CharacterSessionTest.waitFor(() -> !caster.connected() && server.players() == 1
                        && server.connections() == 1 && observer.snapshot().actors().stream().noneMatch(a -> a.id() == old), observer);
                Task201CharacterSessionTest.waitFor(() -> lab.departureCallbacks.get() == 1, observer);
                assertEquals(1, lab.logicalRemovals.get()); assertEquals(0, lab.missingPrivateSnapshots.get());
                assertEquals("coop-world", lab.ownerThread); assertNull(caster.skills());
                assertFalse(caster.castSkill(5, 0, 0, 0, 0)); assertFalse(caster.send(Protocol.Action.MOVE, 1, 0));
                view.actors(observer.snapshot()); assertFalse(view.hidden(old));
                assertEquals(observerApplication == 7 ? 1 : 0, view.activeCount(), "A fase do outro personagem permanece");

                Object original = lab.owner(lab::blockAdmission);
                try { assertThrows(java.io.IOException.class, caster::reconnect); }
                finally { lab.owner(() -> { lab.geometry(original); return true; }); }
                Task201CharacterSessionTest.waitFor(() -> server.connections() == 1, observer);
                assertEquals(1, lab.owner(() -> lab.world.snapshot().actors().size()));
                try (var next = caster.reconnect()) {
                    next.revisions().renderer(p -> CompletableFuture.completedFuture(null));
                    Task201CharacterSessionTest.ready(next, observer);
                    Task201CharacterSessionTest.waitFor(() -> next.skills() != null && next.playerSheet() != null, next, observer);
                    assertNotEquals(old, next.id()); assertEquals("none", next.playerSheet().progress().domain());
                    assertEquals(List.of(0, 0, 0, 0, 0), next.skills().cooldowns());
                    assertTrue(lab.owner(() -> {
                        assertNotEquals(oldIdentity, lab.player(next.id()).networkState().identity().id());
                        assertNull(lab.player(old)); assertFalse(lab.retire(token));
                        assertTrue(lab.world.gameplay(next.id(), new GameplayProtocol.Request(100,
                                GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder")));
                        assertFalse(lab.world.skill(next.id(), new SkillProtocol.Request(100, 5, 0, 0, 0, 0)));
                        assertEquals(observerIdentity, lab.player(observer.id()).networkState().identity().id());
                        return true;
                    }));
                    long before = observer.snapshot().tick();
                    Task201CharacterSessionTest.waitFor(() -> observer.snapshot().tick() > before + 3, next, observer);
                    assertEquals(2, server.players()); assertNull(server.failure()); assertTrue(observer.connected());
                }
            }
        }
    }

    /** Fechamento pelo jogador durante a fase converge para leave único e invalida o candidato. */
    @Test void spontaneousDisconnectDuringPhaseDoesNotRepeatLogicalDeparture() throws Exception {
        var lab = new Lab();
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, lab.simulation)) {
            server.start();
            try (var caster = connect(server, "Espontanea R2", 7); var observer = connect(server, "Testemunha R2", 7)) {
                Task201CharacterSessionTest.ready(caster, observer);
                assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN, "thunder"));
                Task201CharacterSessionTest.waitFor(() -> caster.playerSheet().progress().domain().equals("thunder"), caster, observer);
                assertTrue(caster.castSkill(5, 0, 0, 0, 0));
                Task201CharacterSessionTest.waitFor(() -> caster.skills() != null && !caster.skills().events().isEmpty(), caster, observer);
                var token = lab.owner(() -> lab.token(caster.id()));
                assertTrue(lab.owner(() -> token.world().skillIntangible()));
                caster.close();
                Task201CharacterSessionTest.waitFor(() -> lab.departureCallbacks.get() == 1
                        && observer.snapshot().actors().stream().noneMatch(a -> a.id() == caster.id()), observer);
                assertTrue(lab.owner(() -> {
                    assertFalse(lab.retire(token)); assertFalse(lab.retire(token));
                    assertFalse(token.world().skillIntangible()); assertFalse(token.world().skillInDash());
                    assertNull(lab.player(caster.id())); return true;
                }));
                assertEquals(1, lab.logicalRemovals.get()); assertEquals(0, lab.missingPrivateSnapshots.get());
                assertEquals(1, server.players()); assertTrue(observer.connected()); assertNull(server.failure());
            }
        }
    }
}
