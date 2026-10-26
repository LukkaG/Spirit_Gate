package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Laboratório TCP ordenado e determinístico; não modela perda de datagramas. */
class Task183NetworkLabTest {
    @TempDir Path root;

    static final class Relay implements AutoCloseable {
        final ServerSocket listener;
        final Thread accept;
        final List<Thread> pumps = new CopyOnWriteArrayList<>();
        volatile Socket front, back;
        volatile boolean closed;
        volatile Throwable failure;
        final String profile;

        Relay(int upstream, String profile) throws IOException {
            this.profile = profile;
            listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            accept = new Thread(() -> {
                try {
                    front = listener.accept();
                    back = new Socket(InetAddress.getLoopbackAddress(), upstream);
                    front.setTcpNoDelay(true);
                    back.setTcpNoDelay(true);
                    pump(front, back, false);
                    pump(back, front, true);
                } catch (IOException e) { if (!closed) failure = e; }
            }, "task183-accept");
            accept.setDaemon(true);
            accept.start();
        }

        void pump(Socket from, Socket to, boolean downstream) {
            Thread thread = new Thread(() -> {
                try {
                    var in = new DataInputStream(from.getInputStream());
                    var out = new DataOutputStream(to.getOutputStream());
                    for (int count = 0; !closed; count++) {
                        // Retém a leitura antes de consumir o primeiro frame nesse sentido.
                        if (count == 0 && profile.equals(downstream ? "read-pause" : "write-pause"))
                            Thread.sleep(250);
                        int size = in.readInt();
                        if (size < 1 || size > Protocol.MAX_FRAME) throw new IOException("Frame fora do limite");
                        byte[] bytes = in.readNBytes(size);
                        if (bytes.length != size) throw new EOFException();
                        if (profile.equals("latency")) Thread.sleep(10);
                        if (profile.equals("jitter")) Thread.sleep(new int[]{5, 20, 10, 5}[count % 4]);
                        out.writeInt(size);
                        out.write(bytes);
                        out.flush();
                    }
                } catch (EOFException | SocketException expected) {
                    // EOF/fechamento são estímulos deliberados do laboratório.
                } catch (InterruptedException expected) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) { if (!closed) failure = e; }
            }, "task183-pump-" + downstream);
            thread.setDaemon(true);
            pumps.add(thread);
            thread.start();
        }

        int port() { return listener.getLocalPort(); }

        public void close() throws Exception {
            closed = true;
            listener.close();
            if (front != null) front.close();
            if (back != null) back.close();
            accept.interrupt();
            accept.join(1000);
            // accept terminou: não pode acrescentar pumps depois deste snapshot.
            for (Thread thread : pumps) thread.interrupt();
            for (Thread thread : pumps) { thread.join(1000); assertFalse(thread.isAlive()); }
            assertFalse(accept.isAlive());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"latency", "jitter", "read-pause", "write-pause", "disconnect", "half-close"})
    void twoRevisionClientsRecoverAndReleaseLaboratory(String profile) throws Exception {
        long started = System.nanoTime();
        int relayPort;
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root)) {
            String hash = LauncherSession.managed(server).target().activeHash();
            var relay = new Relay(server.port(), profile);
            relayPort = relay.port();
            try (relay; var secondRelay = new Relay(server.port(), "latency");
                 var a = peer(relay.port(), "Lab A"); var b = peer(secondRelay.port(), "Lab B")) {
                WorldRevisionTest.waitFor(() -> !a.revisions().blocked() && !b.revisions().blocked()
                    && a.snapshot() != null && b.snapshot() != null, a, b);
                assertEquals(hash, a.revisions().hash());
                assertEquals(hash, b.revisions().hash());
                assertEquals(2, server.players());
                float x = server.snapshot().actors().stream().filter(p -> p.id() == a.id()).findFirst().orElseThrow().x();
                assertTrue(a.send(Protocol.Action.MOVE, -1, 0));
                WorldRevisionTest.waitFor(() -> server.snapshot().actors().stream()
                    .anyMatch(p -> p.id() == a.id() && p.x() < x - 5), a, b);
                if (profile.equals("disconnect") || profile.equals("half-close")) {
                    if (profile.equals("half-close")) relay.back.shutdownOutput();
                    else relay.close();
                    WorldRevisionTest.waitFor(() -> server.players() == 1, b);
                    try (var replacement = peer(server.port(), "Lab retorno")) {
                        WorldRevisionTest.waitFor(() -> !replacement.revisions().blocked()
                            && replacement.snapshot() != null, b, replacement);
                        assertEquals(hash, replacement.revisions().hash());
                        assertEquals(2, server.players());
                        assertEquals(2, server.snapshot().actors().stream().filter(p -> !p.boss()).count());
                        assertNotEquals(a.id(), replacement.id());
                    }
                }
                assertNull(server.failure());
                assertNull(relay.failure);
                assertNull(secondRelay.failure);
                assertEquals(hash, LauncherSession.managed(server).target().activeHash());
                System.out.printf("TASK183 scenario=%s status=PASS clients=2 hash=%s durationMs=%.3f%n",
                    profile, hash, (System.nanoTime() - started) / 1_000_000.0);
            }
        }
        try (var rebound = new ServerSocket(relayPort, 1, InetAddress.getLoopbackAddress())) {
            assertEquals(relayPort, rebound.getLocalPort());
        }
    }

    static LocalClient peer(int port, String name) throws Exception {
        var client = LocalClient.revision(InetAddress.getLoopbackAddress(), port, name,
            false, false, null, WorldRevisionTest.EMPTY);
        client.revisions().renderer(p -> CompletableFuture.completedFuture(null));
        return client;
    }
}
