package com.projetok.prototype.rpg;

import com.projetok.multiplayer.*;
import com.projetok.studio.AssetCatalog;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita o loop modal da EDT com servidor real; a ponte apenas retarda ou corta o TCP. */
class SessionGameReconnectCloseTest {
    @TempDir Path temporary;

    @Test void refusalToCloseKeepsTheOriginalWindowAndAllowsAnotherReconnect() throws Exception {
        confirmWhileReconnectCompletes(false);
    }

    @Test void acceptedCloseCannotBeReplacedByThePendingReconnect() throws Exception {
        confirmWhileReconnectCompletes(true);
    }

    private void confirmWhileReconnectCompletes(boolean accepted) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try (var session = new TestSession(temporary)) {
            session.startReconnect();
            JDialog confirmation = session.openCloseConfirmation();
            session.capture(confirmation, accepted ? "accepted-confirmation" : "refused-confirmation");
            session.proxy.release.countDown();
            waitFor(() -> session.adoptions.get() > 1 || session.server.connections() == 0);
            assertEquals(1, session.adoptions.get(), "Reconexao nao pode adotar janela sob a confirmacao");
            assertTrue(onEdt(session.initial::isDisplayable), "A decisao ainda pertence a janela original");
            assertTrue(onEdt(confirmation::isShowing));
            assertTrue(session.server.running());
            assertEquals(0, session.server.connections(), "Cliente da reconexao descartada deve liberar o TCP");
            session.choose(confirmation, accepted);
            if (accepted) {
                waitFor(() -> !session.server.running());
                assertFalse(onEdt(session.initial::isDisplayable));
                assertEquals(1, session.adoptions.get());
            } else {
                assertTrue(session.server.running());
                assertTrue(onEdt(session.initial::isDisplayable));
                waitFor(() -> onEdt(() -> session.reconnect().isEnabled()));
                SwingUtilities.invokeAndWait(() -> session.reconnect().doClick());
                waitFor(() -> session.adoptions.get() == 2);
                assertFalse(onEdt(session.initial::isDisplayable));
                assertTrue(session.server.running(), "Trocar a janela apos recusar o fechamento preserva o host");
                var client = (LocalClient) session.current.get().getRootPane().getClientProperty("networkClient");
                client.await(state -> state.actors().stream().anyMatch(actor -> actor.id() == client.id()), Duration.ofSeconds(8));
                session.capture(session.current.get(), "retry-after-refusal");
            }
            Files.writeString(session.evidence.resolve("result.json"), "{\"realTcpServer\":true,\"acceptedClose\":" + accepted
                    + ",\"noReplacementUnderConfirmation\":true,\"discardedConnectionReleased\":true,\"retryAfterRefusal\":" + !accepted + "}");
        }
    }

    @Test void failedReconnectAfterClosingTheHostCannotResurrectAnErrorDialog() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try (var session = new TestSession(temporary)) {
            session.startReconnect();
            JDialog confirmation = session.openCloseConfirmation();
            session.choose(confirmation, true);
            waitFor(() -> !session.server.running() && !onEdt(session.initial::isDisplayable));
            session.proxy.cutSecondConnection();
            waitFor(() -> onEdt(() -> lateError(session.initial) != null
                    || Boolean.FALSE.equals(session.initial.getRootPane().getClientProperty("networkReconnecting"))));
            assertNull(onEdtDialog(() -> lateError(session.initial)), "Falha tardia nao pode abrir outro modal apos fechar a partida");
            assertEquals(1, session.adoptions.get());
            assertFalse(onEdt(session.initial::isDisplayable));
            Files.writeString(session.evidence.resolve("result.json"), "{\"realTcpServer\":true,\"closedBeforeNetworkFailure\":true,\"noLateErrorDialog\":true}");
        }
    }

    private static final class TestSession implements AutoCloseable {
        final LocalSimulationServer server;
        final DelayedTcpBridge proxy;
        final AtomicInteger adoptions = new AtomicInteger();
        final AtomicReference<JFrame> current = new AtomicReference<>();
        final List<JFrame> windows = new CopyOnWriteArrayList<>();
        final JFrame initial;
        final LocalClient client;
        final Path evidence;
        final String prior;

        TestSession(Path temporary) throws Exception {
            Path project = Path.of("..").toAbsolutePath().normalize();
            evidence = Files.createTempDirectory(Files.createDirectories(project.resolve(".task-checkpoints/TASK-201/corujao-vii/lifecycle-reconnect")), "run-");
            var catalog = CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
            var human = RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"), 250, .5);
            var visuals = new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"), human);
            var preview = catalog.onLogicalCanvas(64, 64, 32, 60, NetworkPlayerVisuals.LEGACY_SCALE);
            var assets = AssetCatalog.scan(project, null);
            prior = System.getProperty("spiritgate.rpg.dataDir");
            System.setProperty("spiritgate.rpg.dataDir", temporary.toString());
            server = LauncherSession.start(InetAddress.getLoopbackAddress(), 0, catalog, project);
            proxy = new DelayedTcpBridge(server.port());
            var choice = new SessionCharacterSelection("Teste reconexao", "human", "none", "none");
            client = LocalClient.character(InetAddress.getLoopbackAddress(), proxy.port(), choice, false, false, null, assets);
            var lifecycle = new SessionGameLifecycle(server, project);
            SwingUtilities.invokeAndWait(() -> RpgNetworkClientLauncher.open(client, human, "127.0.0.1:" + proxy.port(), catalog,
                    choice.cosmetics(), frame -> {
                        lifecycle.adopt(frame); current.set(frame); windows.add(frame); adoptions.incrementAndGet();
                    }, new RpgNetworkClientLauncher.Presentation(visuals, preview)));
            initial = current.get();
            client.await(state -> state.actors().stream().anyMatch(actor -> actor.id() == client.id()), Duration.ofSeconds(8));
        }

        JButton reconnect() { return findButton(initial, "Reconectar nova sess\u00e3o"); }

        void startReconnect() throws Exception {
            client.close();
            waitFor(() -> server.connections() == 0 && onEdt(() -> reconnect().isEnabled()));
            SwingUtilities.invokeAndWait(() -> reconnect().doClick());
            assertTrue(proxy.secondAccepted.await(2, TimeUnit.SECONDS));
            waitFor(() -> server.players() == 1);
        }

        JDialog openCloseConfirmation() throws Exception {
            SwingUtilities.invokeLater(() -> initial.dispatchEvent(new WindowEvent(initial, WindowEvent.WINDOW_CLOSING)));
            waitFor(() -> onEdt(() -> confirmation(initial) != null));
            return onEdtDialog(() -> confirmation(initial));
        }

        void choose(JDialog dialog, boolean accepted) throws Exception {
            SwingUtilities.invokeAndWait(() -> findPane(dialog).setValue(accepted ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION));
            waitFor(() -> !onEdt(dialog::isShowing));
        }

        void capture(Window window, String name) throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                var image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics(); window.printAll(graphics); graphics.dispose();
                try { ImageIO.write(image, "png", evidence.resolve(name + ".png").toFile()); }
                catch (IOException error) { throw new RuntimeException(error); }
            });
        }

        @Override public void close() throws Exception {
            proxy.close(); client.close();
            SwingUtilities.invokeAndWait(() -> { for (JFrame frame : windows) disposeOwned(frame); });
            server.close();
            if (prior == null) System.clearProperty("spiritgate.rpg.dataDir"); else System.setProperty("spiritgate.rpg.dataDir", prior);
        }
    }

    /** Encaminha bytes sem decodificar ou inventar mensagens; somente a segunda resposta aguarda o teste. */
    private static final class DelayedTcpBridge implements AutoCloseable {
        final CountDownLatch secondAccepted = new CountDownLatch(1), release = new CountDownLatch(1);
        final ServerSocket listener = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
        final ExecutorService io = Executors.newCachedThreadPool(task -> { var thread = new Thread(task, "task201-tcp-bridge"); thread.setDaemon(true); return thread; });
        final List<Socket> sockets = new CopyOnWriteArrayList<>();
        volatile Socket secondClient, secondServer;

        DelayedTcpBridge(int targetPort) throws IOException {
            io.execute(() -> {
                int count = 0;
                while (!listener.isClosed()) {
                    try {
                        var downstream = listener.accept(); sockets.add(downstream);
                        var upstream = new Socket(InetAddress.getLoopbackAddress(), targetPort); sockets.add(upstream);
                        downstream.setTcpNoDelay(true); upstream.setTcpNoDelay(true);
                        boolean second = ++count == 2;
                        if (second) { secondClient = downstream; secondServer = upstream; secondAccepted.countDown(); }
                        io.execute(() -> pump(downstream, upstream, false));
                        io.execute(() -> pump(upstream, downstream, second));
                    } catch (IOException ignored) { if (!listener.isClosed()) closeListener(); }
                }
            });
        }

        int port() { return listener.getLocalPort(); }

        private void pump(Socket source, Socket destination, boolean delayed) {
            try {
                if (delayed && !release.await(8, TimeUnit.SECONDS)) return;
                source.getInputStream().transferTo(destination.getOutputStream());
            } catch (IOException ignored) {
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt();
            } finally { closeSocket(source); closeSocket(destination); }
        }

        void cutSecondConnection() { closeSocket(secondClient); closeSocket(secondServer); release.countDown(); }
        private void closeListener() { try { listener.close(); } catch (IOException ignored) { } }
        @Override public void close() { closeListener(); sockets.forEach(DelayedTcpBridge::closeSocket); release.countDown(); io.shutdownNow(); }
        private static void closeSocket(Socket socket) { if (socket != null) try { socket.close(); } catch (IOException ignored) { } }
    }

    private static JDialog confirmation(JFrame owner) {
        for (Window window : owner.getOwnedWindows()) if (window instanceof JDialog dialog && dialog.isShowing()
                && "Encerrar sessao".equals(dialog.getTitle())) return dialog;
        return null;
    }

    private static JDialog lateError(JFrame owner) {
        for (Window window : owner.getOwnedWindows()) if (window instanceof JDialog dialog && dialog.isShowing()) {
            var pane = findPane(dialog);
            if (pane != null && String.valueOf(pane.getMessage()).startsWith("Reconex\u00e3o recusada:")) return dialog;
        }
        return null;
    }

    private static JOptionPane findPane(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JOptionPane pane) return pane;
            if (component instanceof Container nested) { var pane = findPane(nested); if (pane != null) return pane; }
        }
        return null;
    }

    private static JButton findButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container nested) { var button = findButton(nested, text); if (button != null) return button; }
        }
        return null;
    }

    private static void disposeOwned(Window window) { for (Window child : window.getOwnedWindows()) disposeOwned(child); window.dispose(); }
    private static boolean onEdt(BooleanSupplier action) { boolean[] result = {false}; try { SwingUtilities.invokeAndWait(() -> result[0] = action.getAsBoolean()); } catch (Exception error) { throw new RuntimeException(error); } return result[0]; }
    private static JDialog onEdtDialog(java.util.function.Supplier<JDialog> action) { JDialog[] result = {null}; try { SwingUtilities.invokeAndWait(() -> result[0] = action.get()); } catch (Exception error) { throw new RuntimeException(error); } return result[0]; }
    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertTrue(condition.getAsBoolean(), "Condicao de janela ou TCP nao confirmou no prazo");
    }
}
