package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.*;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class Task191LifecycleTest {
    @TempDir Path root;
    @Test void queuedOperationCompletesExceptionallyWhenOwnerIsStopped() throws Exception {
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root)) {
            var target=LauncherSession.managed(server).target();
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            var owner=(ScheduledExecutorService)Task187BackpressureTest.field(server,"owner");
            owner.submit(()->{entered.countDown();try{release.await();}catch(InterruptedException expected){Thread.currentThread().interrupt();}});
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            try {
                var pending=target.reserve(target.activeHash(),new WorldRevisionTest().changed(1,500));
                server.close();
                assertTrue(pending.isCompletedExceptionally(),"Reserva enfileirada nao pode ficar pendente apos shutdown");
                assertTrue(target.confirmReserved().isCompletedExceptionally());
                assertFalse(server.running());
            } finally {release.countDown();}
        }
    }
    @ParameterizedTest
    @ValueSource(strings={"WAITING_CLIENTS","READY","VERIFYING"})
    void closingDuringTransactionNeverRestartsUnconfirmedContent(String phase) throws Exception {
        String baseline;
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var raw=new WorldRevisionTest.RawPeer(server)) {
            WorldRevisionTest.waitFor(()->raw.resumed);var managed=LauncherSession.managed(server);var target=managed.target();baseline=target.activeHash();
            raw.ready=!phase.equals("WAITING_CLIENTS");raw.activate=!phase.equals("VERIFYING");
            var reservation=target.reserve(baseline,new WorldRevisionTest().changed(1,500));CompletableFuture<?> pending=null;
            if(phase.equals("WAITING_CLIENTS")){WorldRevisionTest.waitFor(()->target.status().phase()==RevisionCoordinator.Phase.WAITING_CLIENTS);pending=reservation;}
            else {assertTrue(reservation.get(5,TimeUnit.SECONDS).accepted());if(phase.equals("VERIFYING")){pending=target.activateReserved();WorldRevisionTest.waitFor(()->target.status().phase()==RevisionCoordinator.Phase.VERIFYING);}}
            server.close();
            if(pending!=null)assertTrue(pending.isCompletedExceptionally());
            assertEquals("",managed.store().current());
        }
        try(var restarted=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var joined=Task183NetworkLabTest.peer(restarted.port(),"Retomada")) {
            WorldRevisionTest.waitFor(()->!joined.revisions().blocked(),joined);
            assertEquals(baseline,joined.revisions().hash());assertEquals(baseline,LauncherSession.managed(restarted).target().activeHash());
        }
        System.out.println("TASK191 shutdownPhase="+phase+" restartOnlyConfirmed=true");
    }
    @Test void refusedPreviewConnectionCannotCancelItsReservation() throws Exception {
        try(var preview=new PreviewServer(WorldContent.baseline(),"reaper",1)) {
            assertTrue(preview.reserve(preview.activeHash(),new WorldRevisionTest().changed(1,500)).get(5,TimeUnit.SECONDS).accepted());
            try(var refused=new Socket(InetAddress.getLoopbackAddress(),preview.port())) {
                refused.setSoTimeout(3000);Protocol.write(refused.getOutputStream(),new Protocol.Hello(1,"Recusado"));
                assertInstanceOf(Protocol.Error.class,Protocol.read(refused.getInputStream()));
            }
            Thread.sleep(100);
            assertTrue(preview.activateReserved().get(5,TimeUnit.SECONDS).accepted(),"Saida desconhecida deve preservar reserva");
            assertTrue(preview.abortReserved().get(5,TimeUnit.SECONDS).accepted());
        }
    }
    @Test void abruptChildProcessLeavesJournalAndOnlyExplicitRecoveryCanRestart() throws Exception {
        Path project=root.resolve("crash"),log=root.resolve("child-process.log");
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        var child=new ProcessBuilder(java,"-Djava.awt.headless=true","-Dfile.encoding=UTF-8","-cp",System.getProperty("java.class.path"),Task191LifecycleProbe.class.getName(),project.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(child.waitFor(30,TimeUnit.SECONDS),"Filho deve atingir halt delimitado");
            System.out.print(Files.readString(log));assertEquals(23,child.exitValue());
            var store=new LivePublishing(LauncherSession.revisionRoot(project,0));
            String expected=new WorldRevisionTest().changed(1,500).hash();assertEquals(expected,store.current());
            assertTrue(Files.exists(store.root().resolve("pending.properties")));
            assertThrows(IllegalStateException.class,()->LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,project));
            store.recoverPreviousPointer();assertEquals(expected,store.current());
            try(var files=Files.list(store.root())){assertTrue(files.anyMatch(p->p.getFileName().toString().startsWith("recovered-")));}
            try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,project);var joined=Task183NetworkLabTest.peer(server.port(),"Recuperado")) {
                WorldRevisionTest.waitFor(()->!joined.revisions().blocked(),joined);
                assertEquals(expected,joined.revisions().hash());assertEquals(expected,LauncherSession.managed(server).target().activeHash());
            }
        } finally {if(child.isAlive())child.destroyForcibly();}
        System.out.println("TASK191 processCrash=true powerFailureTested=false confirmedRevisionRecovered=true");
    }
    @Test void closingStudioClosesOwnedPreviewAndKeepsHostAlive() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var host=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);
            var preview=new PreviewServer(WorldContent.baseline(),"reaper",1)) {
            WorldStudio[] ui={null};
            try {
                SwingUtilities.invokeAndWait(()->{try {
                    ui[0]=new WorldStudio(root);var managed=LauncherSession.managed(host);ui[0].authorizePublication(managed.target(),managed.store());
                    Object tools=Task187BackpressureTest.field(ui[0],"liveTools");var field=tools.getClass().getDeclaredField("preview");field.setAccessible(true);field.set(tools,preview);ui[0].setVisible(true);
                }catch(Exception error){throw new RuntimeException(error);}});
                // Etapa posterior à decisão de descarte: o diálogo é coberto pelos testes de arquivos.
                SwingUtilities.invokeAndWait(()->ui[0].dispose());
                SwingUtilities.invokeAndWait(()->{});
                WorldRevisionTest.waitFor(()->!Files.exists(preview.temporary()));
                assertFalse(ui[0].isDisplayable());assertTrue(host.running());
            } finally {if(ui[0]!=null)SwingUtilities.invokeAndWait(()->ui[0].dispose());}
        }
        System.out.println("TASK191 realSwingStudioClosed=true previewClosed=true hostPreserved=true");
    }
}
