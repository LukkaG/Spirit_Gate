package com.projetok.prototype;

import com.projetok.domain.skill.SkillCastCommand;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.HashSet;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Integração da intenção, simulação, renderização e limpeza da Criostase. */
class CryostasisIntegrationTest {
    private static final float DT = 1f / 30f;

    /** Publica um lançamento dentro do tick, com a posição capturada no comando. */
    private void cast(DebugWorld world, float x, float y) {
        world.tickQueued(DebugInput.NONE, DT, 0, List.of(new SkillCastCommand("cryostasis", x, y)));
    }

    /** Integra vigor, pulso imediato e dano flutuante, sem danificar alvos distantes. */
    @Test void lancamentoPublicaZonaEDano() {
        var world = new DebugWorld();
        cast(world, 215, 148);
        var s = world.snapshot();
        assertEquals(45, s.stamina()); assertEquals(3, s.cryostasisCooldown());
        assertEquals(1, s.iceZones().size());
        assertEquals(113, s.actors().get(1).hp()); assertEquals(120, s.actors().get(2).hp());
        assertEquals(7, s.effects().get(0).damage());
        world.tick(new DebugInput(false, false, false, true, true), DT);
        assertTrue(world.snapshot().actors().get(0).x() > s.actors().get(0).x());
        assertNotNull(world.snapshot().attack());
        assertEquals(45.5f, world.snapshot().stamina());
    }

    /** Rejeição de skill não muda vigor ou cooldown e mantém o mundo executando. */
    @Test void entradaInvalidaECooldown() {
        var world = new DebugWorld();
        cast(world, Float.NaN, 0);
        assertEquals(100, world.snapshot().stamina());
        assertEquals(0, world.snapshot().cryostasisCooldown());
        cast(world, 215, 148);
        cast(world, 300, 300);
        assertEquals(45.5f, world.snapshot().stamina());
        assertEquals(1, world.snapshot().iceZones().size());
        assertTrue(world.snapshot().lastEvent().contains("recarga"));
    }

    /** Parede não bloqueia lançamento; a zona visual é removida após 2,5 s. */
    @Test void paredesNaoBloqueiamEZonaExpira() {
        var world = new DebugWorld();
        cast(world, 350, 250); // interior do primeiro obstáculo, dentro de 240 do jogador.
        assertEquals(350, world.snapshot().iceZones().get(0).x());
        assertEquals(250, world.snapshot().iceZones().get(0).y());
        for (int i = 0; i < 75; i++) world.tick(DebugInput.NONE, DT);
        assertTrue(world.snapshot().iceZones().isEmpty());
        assertTrue(world.snapshot().cryostasisCooldown() > 0);
    }

    /** Reinício elimina zonas e cooldown, e descarta comandos anteriores ao reset. */
    @Test void reinicioSemEstadoOuComandosPendentes() throws Exception {
        try (var runtime = new DebugRuntime()) {
            runtime.cast(new SkillCastCommand("cryostasis", 215, 148));
            runtime.restart();
            runtime.start();
            await(() -> runtime.snapshot().ticks() >= 2);
            assertEquals(100, runtime.snapshot().stamina());
            assertTrue(runtime.snapshot().iceZones().isEmpty());
            assertEquals(0, runtime.snapshot().cryostasisCooldown());
            runtime.cast(new SkillCastCommand("cryostasis", 215, 148));
            await(() -> !runtime.snapshot().iceZones().isEmpty());
            runtime.restart();
            await(() -> runtime.snapshot().iceZones().isEmpty());
            assertEquals(100, runtime.snapshot().stamina());
            assertEquals(0, runtime.snapshot().cryostasisCooldown());
        }
    }

    /** A fila preserva comandos rápidos e a ordem em torno de um reinício. */
    @Test void filaPreservaContratoELimpaNoReinicio() {
        var buffer = new DebugCommandBuffer();
        var command = new SkillCastCommand("cryostasis", 10, 20);
        buffer.cast(command);
        assertEquals(List.of(command), buffer.drain().casts());
        assertTrue(buffer.drain().casts().isEmpty());
        buffer.cast(command); buffer.restart();
        assertTrue(buffer.drain().casts().isEmpty());
        buffer.restart(); buffer.cast(command);
        assertEquals(List.of(command), buffer.drain().casts());
    }

    /** Q não perde toque rápido e não repete ao manter, mesmo depois do cooldown. */
    @Test void mouseEQRealSemDuplicacao() throws Exception {
        try (var runtime = new DebugRuntime()) {
            var panel = new DebugPanel(runtime::snapshot);
            SwingUtilities.invokeAndWait(() -> {
                DebugLauncher.configureKeys(panel, runtime, new HashSet<>());
                var event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 215, 148, 0, false);
                for (var listener : panel.getMouseMotionListeners()) listener.mouseMoved(event);
            });
            action(panel, "press_Q"); action(panel, "release_Q");
            runtime.start();
            await(() -> !runtime.snapshot().iceZones().isEmpty());
            assertEquals(45, runtime.snapshot().stamina(), 1.5);
            action(panel, "press_Q"); // recusado em cooldown, mas continua mantido.
            await(() -> runtime.snapshot().cryostasisCooldown() == 0);
            action(panel, "press_Q"); action(panel, "press_Q");
            long tick = runtime.snapshot().ticks();
            await(() -> runtime.snapshot().ticks() >= tick + 2);
            assertEquals(0, runtime.snapshot().cryostasisCooldown());
            assertTrue(runtime.snapshot().iceZones().isEmpty());
            action(panel, "release_Q"); action(panel, "press_Q");
            await(() -> runtime.snapshot().cryostasisCooldown() > 0);
        }
    }

    /** A renderização não executa gameplay e gera captura com a zona em funcionamento. */
    @Test void capturaDaZonaESnapshotImutavel() throws Exception {
        var world = new DebugWorld(); cast(world, 215, 148);
        var before = world.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> before.iceZones().clear());
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { new DebugPanel(world::snapshot).render(graphics, before); }
        finally { graphics.dispose(); }
        assertEquals(before, world.snapshot());
        Files.createDirectories(Path.of("target"));
        ImageIO.write(image, "png", Path.of("target/task004-criostase.png").toFile());
    }

    /** Executa atalho no EDT sem abrir janela. */
    private void action(DebugPanel panel, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.getActionMap().get(name)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name)));
    }
    /** Espera somente publicação da thread da simulação, com prazo finito. */
    private void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 4_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        assertTrue(condition.getAsBoolean(), "Estado deve ser publicado em até 4 s");
    }
}
