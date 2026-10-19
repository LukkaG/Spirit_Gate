package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Integração da interface sem JFrame, com a thread real da simulação. */
class StaminaInterfaceTest {
    /** Um toque completo no EDT antes do primeiro tick deve chegar à simulação. */
    @Test
    void preservaToqueRealEntreTicks() throws Exception {
        try (DebugRuntime runtime = new DebugRuntime()) {
            DebugPanel panel = new DebugPanel(runtime::snapshot);
            SwingUtilities.invokeAndWait(() -> DebugLauncher.configureKeys(panel, runtime, new HashSet<>()));
            action(panel, "press_E"); action(panel, "release_E");
            runtime.start();
            await(() -> runtime.snapshot().stamina() < 90);
            assertTrue(runtime.snapshot().stamina() >= 80);
        }
    }
    /** Ligações de E, repetição do teclado e reinício usam a entrada real do runtime. */
    @Test
    void teclaEReinicioERepeticaoDoTeclado() throws Exception {
        try (DebugRuntime runtime = new DebugRuntime()) {
            DebugPanel panel = new DebugPanel(runtime::snapshot);
            SwingUtilities.invokeAndWait(() -> DebugLauncher.configureKeys(panel, runtime, new HashSet<>()));
            assertEquals("press_E", panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke("pressed E")));
            assertEquals("release_E", panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke("released E")));
            runtime.start();
            await(() -> runtime.snapshot().ticks() >= 2);
            action(panel, "press_E");
            await(() -> runtime.snapshot().stamina() < 90);
            float spent = runtime.snapshot().stamina();
            long tick = runtime.snapshot().ticks();
            for (int i = 0; i < 10; i++) action(panel, "press_E");
            await(() -> runtime.snapshot().ticks() >= tick + 3);
            assertTrue(runtime.snapshot().stamina() >= spent);
            action(panel, "restart");
            await(() -> runtime.snapshot().stamina() == 100);
            long restartedTick = runtime.snapshot().ticks();
            await(() -> runtime.snapshot().ticks() >= restartedTick + 3);
            assertEquals(100, runtime.snapshot().stamina());
            action(panel, "release_E");
            long releaseTick = runtime.snapshot().ticks();
            await(() -> runtime.snapshot().ticks() >= releaseTick + 2);
            action(panel, "press_E");
            await(() -> runtime.snapshot().stamina() < 90);
        }
    }

    /** Barra muda de largura após consumo e produz uma imagem para revisão visual. */
    @Test
    void barraRefleteVigorDoSnapshot() throws Exception {
        var world = new DebugWorld();
        BufferedImage full = render(world.snapshot());
        world.tick(new DebugInput(false, false, false, false, false, true), 1f / 30f);
        BufferedImage depleted = render(world.snapshot());
        int yellow = new Color(235, 190, 65).getRGB();
        int x = DebugWorld.WIDTH + 27;
        assertEquals(yellow, full.getRGB(x + 215, 196));
        assertNotEquals(yellow, depleted.getRGB(x + 215, 196));
        assertEquals(yellow, depleted.getRGB(x + 100, 196));
        Files.createDirectories(Path.of("target"));
        ImageIO.write(depleted, "png", Path.of("target/task003-vigor.png").toFile());
    }

    /** Renderiza somente os dados publicados, sem tocar nas entidades. */
    private BufferedImage render(DebugSnapshot snapshot) {
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        try {
            new DebugPanel(() -> snapshot).render(g, snapshot);
        } finally { g.dispose(); }
        return image;
    }

    /** Executa o mesmo atalho da janela no EDT. */
    private void action(DebugPanel panel, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.getActionMap().get(name)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name)));
    }

    /** Aguarda publicação pela simulação com prazo finito, sem acessar seu estado mutável. */
    private void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        assertTrue(condition.getAsBoolean(), "A simulação deve publicar o estado esperado em 3 s");
    }
}
