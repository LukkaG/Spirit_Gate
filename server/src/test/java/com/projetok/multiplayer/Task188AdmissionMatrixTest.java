package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Matriz cruzada de capacidades sem expulsar peers históricos para liberar Save. */
class Task188AdmissionMatrixTest {
    @TempDir Path root;

    static Protocol.Message snapshot(Socket socket) throws Exception {
        return Protocol.read(socket.getInputStream());
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3})
    void historicalPeerBlocksSaveAndCannotDowngradeCommittedContent(int app) throws Exception {
        var catalog = CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(),0,catalog,root);
             var current = Task183NetworkLabTest.peer(server.port(),"Atualizado")) {
            WorldRevisionTest.waitFor(() -> !current.revisions().blocked(),current);
            var managed = LauncherSession.managed(server);
            String old = managed.target().activeHash();
            var candidate = new WorldRevisionTest().changed(1,500);
            try (var legacy = new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                legacy.setSoTimeout(4000);
                Protocol.write(legacy.getOutputStream(),new Protocol.Hello(app,"Historico"));
                assertInstanceOf(Protocol.Welcome.class,snapshot(legacy));
                Class<?> type = app==1 ? Protocol.State.class : app==2 ? Protocol.SessionState.class : Protocol.CombatState.class;
                assertEquals(type,snapshot(legacy).getClass());
                var blocked = managed.store().publish(candidate,"",old,managed.target(),LivePublishing.Fault.NONE);
                assertFalse(blocked.active());
                assertTrue(blocked.message().contains("Cliente"),blocked.message());
                assertEquals(2,server.players());
                Protocol.write(legacy.getOutputStream(),new Protocol.Intent(1,Protocol.Action.MOVE,0,0));
                assertEquals(type,snapshot(legacy).getClass());
                assertEquals(old,managed.target().activeHash());
                assertEquals("",managed.store().current());
            }
            WorldRevisionTest.waitFor(() -> server.players()==1,current);
            assertTrue(managed.store().publish(candidate,"",old,managed.target(),LivePublishing.Fault.NONE).active());
            WorldRevisionTest.waitFor(() -> !current.revisions().blocked(),current);
            try (var rejected = new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                rejected.setSoTimeout(4000);
                Protocol.write(rejected.getOutputStream(),new Protocol.Hello(app,"Antigo retorno"));
                var error = assertInstanceOf(Protocol.Error.class,snapshot(rejected));
                assertTrue(error.reason().contains("V4"),error.reason());
            }
            WorldRevisionTest.waitFor(() -> server.connections()==1,current);
            try (var joined = Task183NetworkLabTest.peer(server.port(),"Novo V4")) {
                WorldRevisionTest.waitFor(() -> !joined.revisions().blocked() && joined.snapshot()!=null,current,joined);
                assertEquals(candidate.hash(),joined.revisions().hash());
                assertEquals(candidate.hash(),current.revisions().hash());
                assertEquals(candidate.hash(),managed.store().current());
                assertEquals(2,server.players());
            }
            assertNull(server.failure());
            System.out.println("TASK188 legacy="+app+" originalSnapshot=true blockedSave=true keptConnected=true postCommitRejected=true newV4=true");
        }
    }

    @Test void allApplicationsAreRejectedDuringReadyAndAbortPreservesBase() throws Exception {
        var catalog = CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));
        try (var server = LauncherSession.start(InetAddress.getLoopbackAddress(),0,catalog,root);
             var current = Task183NetworkLabTest.peer(server.port(),"Atualizado")) {
            WorldRevisionTest.waitFor(() -> !current.revisions().blocked(),current);
            var managed = LauncherSession.managed(server);
            String old = managed.target().activeHash();
            assertTrue(managed.target().reserve(old,new WorldRevisionTest().changed(1,500)).get(5,TimeUnit.SECONDS).accepted());
            for (int app=1; app<=4; app++) {
                try (var joining = new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                    joining.setSoTimeout(4000);
                    Protocol.write(joining.getOutputStream(),new Protocol.Hello(app,"Durante barreira"));
                    var error = assertInstanceOf(Protocol.Error.class,snapshot(joining));
                    assertTrue(error.reason().contains("publicacao"),error.reason());
                }
                WorldRevisionTest.waitFor(() -> server.connections()==1,current);
                Thread.sleep(100); // Dar ao owner oportunidade de processar a saída recusada.
                assertEquals(RevisionCoordinator.Phase.READY,managed.target().status().phase());
                assertEquals(old,managed.target().activeHash());
            }
            assertTrue(managed.target().abortReserved().get(5,TimeUnit.SECONDS).accepted());
            WorldRevisionTest.waitFor(() -> !current.revisions().blocked(),current);
            assertEquals(old,current.revisions().hash());
            assertEquals(old,managed.target().activeHash());
            assertTrue(current.connected());
            assertEquals("",managed.store().current());
            assertNull(server.failure());
            System.out.println("TASK188 READY applications=1..4 rejected=true baseRestored=true");
        }
    }
}
