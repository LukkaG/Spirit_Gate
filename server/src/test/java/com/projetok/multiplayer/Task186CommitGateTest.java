package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Injeção local na fronteira de confirmação; não representa ACK administrativo pela rede. */
class Task186CommitGateTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void negativeFinalAcknowledgmentCannotReportSuccessfulCommit(boolean acceptedWithWrongHash) throws Exception {
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root);
             var client = Task183NetworkLabTest.peer(server.port(), "Sessao sintetica")) {
            WorldRevisionTest.waitFor(() -> !client.revisions().blocked(), client);
            var managed = LauncherSession.managed(server);
            var delegate = managed.target();
            String old = delegate.activeHash();
            var next = new WorldRevisionTest().changed(1, 500);
            PublicationTarget rejecting = new PublicationTarget() {
                public String activeHash() { return delegate.activeHash(); }
                public String compatibility() { return delegate.compatibility(); }
                public CompletableFuture<PreviewServer.Ack> reserve(String expected, LiveRevision candidate) {
                    return delegate.reserve(expected, candidate);
                }
                public CompletableFuture<PreviewServer.Ack> activateReserved() { return delegate.activateReserved(); }
                public CompletableFuture<PreviewServer.Ack> abortReserved() { return delegate.abortReserved(); }
                public CompletableFuture<PreviewServer.Ack> confirmReserved() {
                    // Recusa válida na interface local: o servidor real ainda não confirmou.
                    return CompletableFuture.completedFuture(new PreviewServer.Ack(acceptedWithWrongHash, next.number(),
                        acceptedWithWrongHash ? "0".repeat(64) : next.hash(), "Confirmacao invalida por falha injetada", 0, 0));
                }
            };
            var rejected = assertThrows(IllegalStateException.class,
                () -> managed.store().publish(next, "", old, rejecting, LivePublishing.Fault.NONE));
            assertEquals("Destino recusou a confirmacao, desconectou ou reverteu a ativacao", rejected.getMessage());
            WorldRevisionTest.waitFor(() -> !client.revisions().blocked() && client.snapshot() != null, client);
            assertEquals(RevisionCoordinator.Phase.ABORTED, delegate.status().phase());
            assertEquals(old, delegate.activeHash());
            assertEquals(old, client.revisions().hash());
            assertEquals("", managed.store().current());
            assertFalse(Files.exists(managed.store().root().resolve("pending.properties")));
            assertFalse(Files.exists(managed.store().root().resolve("active.txt")));
            assertTrue(managed.store().history().isEmpty(), "Candidata recusada fora do historico confirmado");
            try (var files = Files.walk(managed.store().root())) {
                assertTrue(files.anyMatch(p -> p.getFileName().toString().equals("aborted.properties")));
            }
            assertTrue(client.revisions().accepts(old));
            assertFalse(client.revisions().accepts(next.hash()));
            float x = server.snapshot().actors().stream().filter(p -> p.id() == client.id()).findFirst().orElseThrow().x();
            assertTrue(client.send(Protocol.Action.MOVE, -1, 0));
            WorldRevisionTest.waitFor(() -> server.snapshot().actors().stream()
                .anyMatch(p -> p.id() == client.id() && p.x() < x - 5), client);
            assertEquals(old, delegate.activeHash());
            assertEquals(old, client.revisions().hash());
            assertNull(server.failure());
            System.out.printf("TASK186 invalidConfirm accepted=%s hashMatches=%s phase=%s diskEmpty=%s gameplayRecovered=true%n",
                acceptedWithWrongHash, !acceptedWithWrongHash, delegate.status().phase(), managed.store().current().isEmpty());
        }
    }

    @Test void confirmationRequiresVerifiedTransaction() throws Exception {
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root);
             var client = Task183NetworkLabTest.peer(server.port(), "Sessao sintetica")) {
            WorldRevisionTest.waitFor(() -> !client.revisions().blocked(), client);
            var managed = LauncherSession.managed(server);
            var target = managed.target();
            String old = target.activeHash();
            var idleError = assertThrows(ExecutionException.class, () -> target.confirmReserved().get(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, idleError.getCause());
            var next = new WorldRevisionTest().changed(1, 500);
            assertTrue(target.reserve(old, next).get(5, TimeUnit.SECONDS).accepted());
            WorldRevisionTest.waitFor(() -> target.status().phase() == RevisionCoordinator.Phase.READY, client);
            assertEquals(RevisionCoordinator.Phase.READY, target.status().phase());
            var readyError = assertThrows(ExecutionException.class, () -> target.confirmReserved().get(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, readyError.getCause());
            assertTrue(target.abortReserved().get(5, TimeUnit.SECONDS).accepted());
            WorldRevisionTest.waitFor(() -> !client.revisions().blocked(), client);
            assertEquals(old, target.activeHash());
            assertEquals(old, client.revisions().hash());
            assertEquals("", managed.store().current());
            assertNull(server.failure());
            System.out.println("TASK186 invalidPhase IDLE/READY rejected=true previousRevisionPreserved=true");
        }
    }

    @ParameterizedTest
    @EnumSource(value = LivePublishing.Fault.class, names = {"AFTER_STAGE", "AFTER_ACTIVATION", "BEFORE_POINTER"})
    void injectedDiskBoundaryFailuresCannotConfirmCandidate(LivePublishing.Fault fault) throws Exception {
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, null, root);
             var a = Task183NetworkLabTest.peer(server.port(), "Sessao A");
             var b = Task183NetworkLabTest.peer(server.port(), "Sessao B")) {
            WorldRevisionTest.waitFor(() -> !a.revisions().blocked() && !b.revisions().blocked(), a, b);
            var managed = LauncherSession.managed(server);
            String old = managed.target().activeHash();
            var next = new WorldRevisionTest().changed(1, 500);
            assertThrows(IllegalStateException.class, () -> managed.store().publish(next, "", old, managed.target(), fault));
            WorldRevisionTest.waitFor(() -> !a.revisions().blocked() && !b.revisions().blocked(), a, b);
            assertEquals(old, managed.target().activeHash());
            assertEquals(old, a.revisions().hash());
            assertEquals(old, b.revisions().hash());
            assertEquals("", managed.store().current());
            assertEquals(1, managed.store().history().size());
            assertFalse(Files.exists(managed.store().root().resolve("pending.properties")));
            try (var files = Files.walk(managed.store().root())) {
                assertTrue(files.anyMatch(p -> p.getFileName().toString().equals("aborted.properties")));
            }
            assertNull(server.failure());
            System.out.println("TASK186 fault=" + fault + " PASS phase=" + managed.target().status().phase());
        }
    }
}
