package com.projetok.prototype.rpg;

import com.projetok.multiplayer.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Confirma a decisao modal na EDT antes de aplicar resultados de conexoes em andamento. */
class Task201RadminCancellationTest {
    private String priorApp;
    @BeforeEach void projectHome() {
        priorApp=System.getProperty("jpackage.app-path");
        System.setProperty("jpackage.app-path",Path.of("../SpiritGate.exe").toAbsolutePath().normalize().toString());
    }
    @AfterEach void restoreHome() {
        if(priorApp==null)System.clearProperty("jpackage.app-path");else System.setProperty("jpackage.app-path",priorApp);
    }
    interface Check {boolean get() throws Exception;}
    static void waitFor(Check check) throws Exception {
        long until=System.nanoTime()+Duration.ofSeconds(10).toNanos();
        while(!check.get()&&System.nanoTime()<until)Thread.sleep(15);
        assertTrue(check.get(),"A transicao Swing nao confirmou no prazo");
    }
    static <T> T edt(Callable<T> call) throws Exception {
        AtomicReference<T> value=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{try{value.set(call.call());}catch(Throwable e){failure.set(e);}});
        if(failure.get()!=null)throw new AssertionError("Falha na EDT",failure.get());
        return value.get();
    }
    static Object field(Object object,String name) throws Exception {
        Field field=RadminSessionPanel.class.getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }
    static void field(Object object,String name,Object value) throws Exception {
        Field field=RadminSessionPanel.class.getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    static void work(RadminSessionPanel panel,Callable<?> call,Consumer<Object> success) throws Exception {
        Method method=RadminSessionPanel.class.getDeclaredMethod("work",Callable.class,Consumer.class);method.setAccessible(true);method.invoke(panel,call,success);
    }
    static JOptionPane pane(Container root) {
        if(root instanceof JOptionPane value)return value;
        for(Component child:root.getComponents())if(child instanceof Container nested){var value=pane(nested);if(value!=null)return value;}
        return null;
    }
    static JOptionPane confirmation(JDialog owner) {
        for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&dialog.getOwner()==owner){var value=pane(dialog);if(value!=null)return value;}
        return null;
    }
    static void capture(Harness harness,String name) throws Exception {
        String configured=System.getProperty("spiritgate.task201.cancelEvidence");
        if(configured==null)return;
        Path destination=Files.createDirectories(Path.of(configured)).resolve(name+".png");
        edt(()->{
            var pane=confirmation(harness.dialog);
            Window window=pane==null?harness.dialog:SwingUtilities.getWindowAncestor(pane);
            var image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics();window.printAll(graphics);graphics.dispose();
            ImageIO.write(image,"png",destination.toFile());return null;
        });
    }
    static final class OwnedClient implements AutoCloseable {
        final LocalClient client;final AtomicBoolean closed=new AtomicBoolean();
        OwnedClient(LocalClient client){this.client=client;}
        @Override public void close(){closed.set(true);client.close();}
    }
    static final class Harness implements AutoCloseable {
        final LocalSimulationServer server;
        final RadminSessionPanel panel;
        final JFrame owner;
        final JDialog dialog;
        final AtomicReference<Throwable> failure=new AtomicReference<>();
        final List<CountDownLatch> gates=new CopyOnWriteArrayList<>();
        final List<OwnedClient> clients=new CopyOnWriteArrayList<>();
        Harness() throws Exception {
            Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
            server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0);server.start();
            owner=edt(()->{var frame=new JFrame("TASK-201 cancelamento");frame.setSize(600,400);frame.setVisible(true);return frame;});
            panel=edt(()->{var constructor=RadminSessionPanel.class.getDeclaredConstructor(Window.class,boolean.class,String.class);constructor.setAccessible(true);return constructor.newInstance(owner,true,"Anfitriao");});
            dialog=(JDialog)edt(()->field(panel,"dialog"));
            waitFor(()->edt(()->dialog.isShowing()&&!(boolean)field(panel,"busy")));
            assertNotNull(edt(()->field(panel,"resources")),()->"Recursos indisponiveis: "+status());
            edt(()->{field(panel,"server",server);return null;});
        }
        String status(){try{return edt(()->((JLabel)field(panel,"status")).getText());}catch(Exception e){throw new AssertionError(e);}}
        CountDownLatch gate(){var gate=new CountDownLatch(1);gates.add(gate);return gate;}
        void laterStop(boolean closeDialog) {
            SwingUtilities.invokeLater(()->{try{var method=RadminSessionPanel.class.getDeclaredMethod("requestStop",boolean.class);method.setAccessible(true);method.invoke(panel,closeDialog);}catch(Throwable e){failure.set(e);}});
        }
        void decision(boolean yes) throws Exception {
            waitFor(()->edt(()->confirmation(dialog)!=null));
            edt(()->{confirmation(dialog).setValue(yes?JOptionPane.YES_OPTION:JOptionPane.NO_OPTION);return null;});
        }
        OwnedClient connected() throws Exception {
            var value=new OwnedClient(new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Conexao Controlada"));clients.add(value);return value;
        }
        @Override public void close() throws Exception {
            gates.forEach(CountDownLatch::countDown);server.close();clients.forEach(OwnedClient::close);
            edt(()->{field(panel,"closed",true);((Timer)field(panel,"timer")).stop();for(Window w:dialog.getOwnedWindows())w.dispose();dialog.dispose();owner.dispose();return null;});
        }
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void connectionCompletionWaitsForAcceptedOrDeclinedClose(boolean accept) throws Exception {
        try(var h=new Harness()) {
            var release=h.gate();var result=new AtomicReference<OwnedClient>();var consumed=new AtomicInteger();
            edt(()->{work(h.panel,()->{var client=h.connected();result.set(client);assertTrue(release.await(8,TimeUnit.SECONDS));return client;},value->consumed.incrementAndGet());return null;});
            waitFor(()->result.get()!=null);h.laterStop(true);
            waitFor(()->edt(()->confirmation(h.dialog)!=null));release.countDown();
            waitFor(()->edt(()->!((java.util.Deque<?>)field(h.panel,"deferredCompletions")).isEmpty()));
            assertEquals(0,consumed.get());assertFalse(result.get().closed.get());assertTrue(h.server.running());
            capture(h,"close-"+(accept?"accepted":"declined")+"-completion-waits");
            h.decision(accept);
            if(accept) {
                waitFor(()->result.get().closed.get()&&!h.server.running());
                assertEquals(0,consumed.get());assertFalse(edt(h.dialog::isDisplayable));
                assertFalse(result.get().client.connected());
            } else {
                waitFor(()->consumed.get()==1);
                assertTrue(h.server.running());assertTrue(result.get().client.connected());assertFalse(result.get().closed.get());
                assertTrue(edt(h.dialog::isShowing));assertFalse(edt(()->(boolean)field(h.panel,"busy")));
                result.get().close();var retried=new AtomicBoolean();
                edt(()->{work(h.panel,()->"retry",value->retried.set(true));return null;});
                waitFor(retried::get);assertTrue(h.server.running());
            }
            assertNull(h.failure.get());
        }
    }

    @Test void acceptedStopDiscardsLateClientWithoutCompletingNewRetry() throws Exception {
        try(var h=new Harness()) {
            var release=h.gate();var result=new AtomicReference<OwnedClient>();var consumed=new AtomicInteger();
            edt(()->{work(h.panel,()->{var client=h.connected();result.set(client);assertTrue(release.await(8,TimeUnit.SECONDS));return client;},value->consumed.incrementAndGet());return null;});
            waitFor(()->result.get()!=null);h.laterStop(false);
            waitFor(()->edt(()->confirmation(h.dialog)!=null));capture(h,"stop-confirmation-before-late-client");h.decision(true);
            waitFor(()->!h.server.running());assertTrue(edt(h.dialog::isShowing));
            var retryRelease=h.gate();var retryStarted=new CountDownLatch(1);var retryComplete=new AtomicBoolean();
            edt(()->{work(h.panel,()->{retryStarted.countDown();assertTrue(retryRelease.await(8,TimeUnit.SECONDS));return "retry";},value->retryComplete.set(true));return null;});
            assertTrue(retryStarted.await(3,TimeUnit.SECONDS));release.countDown();waitFor(()->result.get().closed.get());
            assertEquals(0,consumed.get());assertFalse(retryComplete.get());assertTrue(edt(()->(boolean)field(h.panel,"busy")));
            retryRelease.countDown();waitFor(retryComplete::get);assertFalse(edt(()->(boolean)field(h.panel,"busy")));
            assertNull(h.failure.get());
        }
    }

    @ParameterizedTest @ValueSource(strings={"EOF","Socket"})
    void statusKeepsContextualUpgradeGuidanceInsteadOfInnermostTransportError(String kind) throws Exception {
        try(var h=new Harness()) {
            String guidance="Confira a conexao e atualize o servidor para criar personagem";
            IOException nativeError=kind.equals("EOF")?new EOFException():new SocketException("Connection reset");
            IOException contextual=new IOException(guidance,nativeError);
            edt(()->{work(h.panel,()->{throw contextual;},value->fail("Nao pode concluir uma conexao recusada"));return null;});
            waitFor(()->h.status().equals(guidance));capture(h,"contextual-upgrade-"+kind);assertFalse(edt(()->(boolean)field(h.panel,"busy")));
            assertTrue(h.server.running());assertNull(h.failure.get());
        }
    }

    @Test void modalDecisionDrainsBothCurrentAndOlderCompletionWithoutLosingResources() throws Exception {
        try(var h=new Harness()) {
            var oldRelease=h.gate();var oldResult=new AtomicReference<OwnedClient>();var oldConsumed=new AtomicInteger();
            edt(()->{work(h.panel,()->{var client=h.connected();oldResult.set(client);assertTrue(oldRelease.await(8,TimeUnit.SECONDS));return client;},value->oldConsumed.incrementAndGet());return null;});
            waitFor(()->oldResult.get()!=null);h.laterStop(false);h.decision(true);waitFor(()->!h.server.running());
            try(var restarted=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)) {
                restarted.start();edt(()->{field(h.panel,"server",restarted);return null;});
                var currentRelease=h.gate();var currentStarted=new CountDownLatch(1);var currentConsumed=new AtomicBoolean();
                edt(()->{work(h.panel,()->{currentStarted.countDown();assertTrue(currentRelease.await(8,TimeUnit.SECONDS));return "nova tentativa";},value->currentConsumed.set(true));return null;});
                assertTrue(currentStarted.await(3,TimeUnit.SECONDS));h.laterStop(true);
                waitFor(()->edt(()->confirmation(h.dialog)!=null));currentRelease.countDown();
                waitFor(()->edt(()->((java.util.Deque<?>)field(h.panel,"deferredCompletions")).size()==1));
                oldRelease.countDown();waitFor(()->edt(()->((java.util.Deque<?>)field(h.panel,"deferredCompletions")).size()==2));
                assertFalse(currentConsumed.get());assertFalse(oldResult.get().closed.get());
                capture(h,"two-worker-completions-wait-for-decision");h.decision(false);
                waitFor(()->currentConsumed.get()&&oldResult.get().closed.get());
                assertEquals(0,oldConsumed.get());assertTrue(restarted.running());
                assertFalse(edt(()->(boolean)field(h.panel,"busy")));assertTrue(edt(h.dialog::isShowing));
            }
            assertNull(h.failure.get());
        }
    }

    @Test void directNativeErrorsAreFriendlyAndOnlyAsyncWrappersAreUnwrapped() {
        assertEquals("Porta ocupada - escolha outra porta",RadminSessionPanel.problemMessage(new ExecutionException(new BindException("bind"))));
        assertEquals("Tempo limite excedido",RadminSessionPanel.problemMessage(new CompletionException(new SocketTimeoutException("timeout"))));
        assertEquals("Servidor indisponivel",RadminSessionPanel.problemMessage(new ExecutionException(new ConnectException("connect"))));
        var context=new IOException("Atualize o servidor",new EOFException());
        assertEquals("Atualize o servidor",RadminSessionPanel.problemMessage(new ExecutionException(new CompletionException(context))));
    }
}
