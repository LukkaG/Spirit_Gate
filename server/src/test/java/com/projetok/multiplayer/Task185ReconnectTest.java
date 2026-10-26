package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.LivePublishing;
import java.net.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Ingresso e retorno reais, incluindo as fronteiras da transferência candidata. */
class Task185ReconnectTest {
    @TempDir Path root;

    static final class CuttingPeer implements AutoCloseable {
        final Socket socket;
        final Thread reader;
        final String cut;
        volatile boolean armed, initialResumed;
        volatile Throwable failure;
        final CountDownLatch disconnected = new CountDownLatch(1);

        CuttingPeer(LocalSimulationServer server, String cut, boolean armed) throws Exception {
            this.cut = cut;
            this.armed = armed;
            socket = new Socket(InetAddress.getLoopbackAddress(), server.port());
            socket.setSoTimeout(7000);
            Protocol.write(socket.getOutputStream(), new Protocol.Hello(4, "Sessao sintetica"));
            assertInstanceOf(Protocol.Welcome.class, Protocol.read(socket.getInputStream()));
            reader = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        var message = Protocol.read(socket.getInputStream());
                        if (!(message instanceof Protocol.Content c)) continue;
                        String point = c.kind().name();
                        if (c.kind() == Protocol.ContentKind.END) {
                            ack(c, Protocol.ContentKind.READY);
                            point = "READY";
                        } else if (c.kind() == Protocol.ContentKind.ACTIVATE) {
                            if (!(this.armed && cut.equals("ACTIVATE"))) ack(c, Protocol.ContentKind.ACTIVE);
                        } else if (c.kind() == Protocol.ContentKind.ABORT) ack(c, Protocol.ContentKind.ACTIVE);
                        if (c.kind() == Protocol.ContentKind.RESUME) initialResumed = true;
                        if (this.armed && cut.equals(point)) {
                            socket.close();
                            disconnected.countDown();
                            return;
                        }
                    }
                } catch (Exception e) { if (!socket.isClosed()) failure = e; }
            }, "task185-cut-peer");
            reader.setDaemon(true);
            reader.start();
        }

        void ack(Protocol.Content c, Protocol.ContentKind kind) throws Exception {
            Protocol.write(socket.getOutputStream(), new Protocol.Content(kind, c.transaction(), c.hash(), 0, new byte[0]));
        }

        public void close() throws Exception {
            socket.close();
            reader.interrupt();
            reader.join(1000);
            assertFalse(reader.isAlive());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEGIN", "READY", "ACTIVATE", "RESUME"})
    void disconnectDuringInitialTransferCannotLeavePartialGameplay(String phase) throws Exception {
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root);
             var survivor = Task183NetworkLabTest.peer(server.port(), "Sobrevivente")) {
            WorldRevisionTest.waitFor(() -> !survivor.revisions().blocked(), survivor);
            String hash = LauncherSession.managed(server).target().activeHash();
            try (var cut = new CuttingPeer(server, phase, true)) {
                assertTrue(cut.disconnected.await(7, TimeUnit.SECONDS));
                WorldRevisionTest.waitFor(() -> server.players() == 1 && !survivor.revisions().blocked(), survivor);
                assertNull(cut.failure);
                reconnect(server, survivor, hash);
                System.out.println("TASK185 initial phase=" + phase + " PASS hash=" + hash);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEGIN", "READY", "ACTIVATE", "RESUME"})
    void disconnectDuringCandidateReturnsOnlyToConfirmedRevision(String phase) throws Exception {
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root);
             var survivor = Task183NetworkLabTest.peer(server.port(), "Sobrevivente");
             var cut = new CuttingPeer(server, phase, false)) {
            WorldRevisionTest.waitFor(() -> cut.initialResumed && !survivor.revisions().blocked(), survivor);
            var managed = LauncherSession.managed(server);
            String old = managed.target().activeHash();
            var candidate = new WorldRevisionTest().changed(1, 500);
            cut.armed = true;
            LivePublishing.Result result = null;
            Exception publicationError = null;
            try {
                result = managed.store().publish(candidate, "", old, managed.target(), LivePublishing.Fault.NONE);
            } catch (Exception e) { publicationError = e; }
            assertTrue(cut.disconnected.await(7, TimeUnit.SECONDS));
            boolean committed = phase.equals("RESUME");
            if (committed) {
                assertNull(publicationError);
                assertNotNull(result);
                assertTrue(result.active());
            } else if (result != null) assertFalse(result.active());
            String expected = committed ? candidate.hash() : old;
            WorldRevisionTest.waitFor(() -> server.players() == 1 && !survivor.revisions().blocked(), survivor);
            assertEquals(committed ? candidate.hash() : "", managed.store().current());
            assertEquals(expected, managed.target().activeHash());
            assertNull(cut.failure);
            reconnect(server, survivor, expected);
            System.out.println("TASK185 candidate phase=" + phase + " PASS committed=" + committed + " hash=" + expected);
        }
    }

    private static void reconnect(LocalSimulationServer server, LocalClient survivor, String expected) throws Exception {
        try (var replacement = Task183NetworkLabTest.peer(server.port(), "Retorno")) {
            WorldRevisionTest.waitFor(() -> !replacement.revisions().blocked()
                && replacement.snapshot() != null && !survivor.revisions().blocked(), survivor, replacement);
            assertEquals(expected, replacement.revisions().hash());
            assertEquals(expected, survivor.revisions().hash());
            assertEquals(2, server.players());
            assertEquals(2, server.snapshot().actors().stream().filter(p -> !p.boss()).count());
            assertNull(server.failure());
        }
    }
}
