package com.projetok.prototype;

import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.domain.skill.LightningStepService;
import org.junit.jupiter.api.Test;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

/** Integração de fila, deslocamento e rastro; o caso de direção zero pertence ao usuário. */
class LightningStepIntegrationTest {
    private static final float DT = 1f / 30f;
    private static final SkillCastCommand DASH = new SkillCastCommand("lightning_step", 500, 340);

    /** Envia um lote pelo mesmo ponto de entrada usado pelo runtime. */
    private void cast(DebugWorld world, SkillCastCommand... commands) {
        world.tickQueued(DebugInput.NONE, DT, 0, List.of(commands));
    }

    /** Ativação não teleporta e o gasto acontece uma vez no início. */
    @Test void ativacaoEProgressoSemTeleporte() {
        var world = new DebugWorld();
        var before = world.snapshot().actors().get(0);
        cast(world, DASH);
        var started = world.snapshot();
        assertEquals(before.x(), started.actors().get(0).x());
        assertEquals(before.y(), started.actors().get(0).y());
        assertEquals(65, started.stamina()); assertEquals(4, started.lightningStepCooldown());
        assertTrue(started.lightningStepActive()); assertTrue(started.lightningTrails().isEmpty());
        world.tick(DebugInput.NONE, DT);
        assertEquals(before.x() + 20, world.snapshot().actors().get(0).x(), 0.0001);
        assertEquals(65.5f, world.snapshot().stamina());
        assertFalse(world.snapshot().lightningTrails().isEmpty());
    }

    /** WASD fica suspenso inclusive no último passo fracionário e retorna depois. */
    @Test void distanciaEDuracaoSemSomarWasd() {
        var world = new DebugWorld(); cast(world, DASH);
        for (int i = 0; i < 8; i++) world.tick(new DebugInput(true, false, false, false, false), DT);
        var done = world.snapshot();
        assertFalse(done.lightningStepActive()); assertEquals(0, done.lightningStepRemaining());
        assertEquals(285, done.actors().get(0).x(), 0.0001);
        assertEquals(340, done.actors().get(0).y());
        world.tick(new DebugInput(true, false, false, false, false), DT);
        assertEquals(340 - DebugWorld.WALK_SPEED * DT, world.snapshot().actors().get(0).y(), 0.0001);
    }

    /** Cooldown impede outro consumo e não impede o ataque básico. */
    @Test void cooldownConsumoUnicoEAtaqueBasico() {
        var world = new DebugWorld(); cast(world, DASH);
        cast(world, DASH);
        assertEquals(65.5f, world.snapshot().stamina());
        world.tick(new DebugInput(false, false, false, false, true), DT);
        assertNotNull(world.snapshot().attack());
        assertEquals(0.38f, world.snapshot().cooldown());
        assertEquals(120, world.snapshot().actors().get(1).hp());
    }

    /** Q e Shift competem pelo saldo conforme a ordem do lote recebido. */
    @Test void concorrenciaEntreHabilidadesRespeitaFila() {
        var ice = new SkillCastCommand("cryostasis", 215, 148);
        var iceFirst = new DebugWorld();
        iceFirst.tick(new DebugInput(false, false, false, false, false, true), DT);
        cast(iceFirst, ice, DASH);
        assertEquals(25.5f, iceFirst.snapshot().stamina());
        assertEquals(1, iceFirst.snapshot().iceZones().size());
        assertFalse(iceFirst.snapshot().lightningStepActive());
        assertEquals(0, iceFirst.snapshot().lightningStepCooldown());
        var dashFirst = new DebugWorld();
        dashFirst.tick(new DebugInput(false, false, false, false, false, true), DT);
        cast(dashFirst, DASH, ice);
        assertEquals(45.5f, dashFirst.snapshot().stamina());
        assertTrue(dashFirst.snapshot().lightningStepActive());
        assertTrue(dashFirst.snapshot().iceZones().isEmpty());
        assertEquals(0, dashFirst.snapshot().cryostasisCooldown());
    }

    /** O avanço do dash não habilita invulnerabilidade nem impede a IA de atacar. */
    @Test void jogadorRecebeDanoDuranteDash() {
        var world = new DebugWorld();
        for (int i = 0; i < 47; i++) world.tick(DebugInput.NONE, DT);
        int hpBefore = world.snapshot().playerHp();
        // O mob se aproxima de cima/direita; avançar nessa direção mantém alcance no primeiro passo.
        cast(world, new SkillCastCommand("lightning_step", 300, 100));
        boolean damaged = world.snapshot().playerHp() < hpBefore;
        for (int i = 0; i < 8 && !damaged; i++) {
            world.tick(DebugInput.NONE, DT);
            damaged = world.snapshot().playerHp() < hpBefore;
        }
        assertTrue(damaged, "O jogador continua elegível ao dano durante a habilidade");
    }

    /** Segmentos permanecem só por seu tempo de vida; a renderização não altera o mundo. */
    @Test void capturaERastroImutavelQueExpira() throws Exception {
        var world = new DebugWorld(); cast(world, DASH);
        for (int i = 0; i < 5; i++) world.tick(DebugInput.NONE, DT);
        var snapshot = world.snapshot();
        assertFalse(snapshot.lightningTrails().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.lightningTrails().clear());
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { new DebugPanel(world::snapshot).render(graphics, snapshot); }
        finally { graphics.dispose(); }
        assertEquals(snapshot, world.snapshot());
        Files.createDirectories(Path.of("target"));
        ImageIO.write(image, "png", Path.of("target/task006-passo-relampago.png").toFile());
        for (int i = 0; i < 12; i++) world.tick(DebugInput.NONE, DT);
        assertFalse(world.snapshot().lightningStepActive());
        assertTrue(world.snapshot().lightningTrails().isEmpty());
    }

    /** Shift usa o modificador real do Swing e não remove as combinações dos controles anteriores. */
    @Test void bindingsComShiftMantemControlesAnteriores() throws Exception {
        try (var runtime = new DebugRuntime()) {
            var panel = panel(runtime);
            var map = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            var pressed = new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, InputEvent.SHIFT_DOWN_MASK,
                    KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED);
            assertEquals("press_SHIFT", map.get(KeyStroke.getKeyStrokeForEvent(pressed)));
            for (String key : new String[] {"W", "A", "S", "D", "SPACE", "E", "Q"}) {
                assertEquals("press_" + key, map.get(KeyStroke.getKeyStroke("shift pressed " + key)));
                assertEquals("release_" + key, map.get(KeyStroke.getKeyStroke("shift released " + key)));
            }
            assertEquals("restart", map.get(KeyStroke.getKeyStroke("shift pressed R")));
            assertEquals("hitboxes", map.get(KeyStroke.getKeyStroke("shift pressed H")));
        }
    }

    /** Um toque completo antes do tick chega ao mundo, e R limpa estado sem repetir Shift mantido. */
    @Test void toqueRapidoReinicioEShiftMantido() throws Exception {
        try (var runtime = new DebugRuntime()) {
            var panel = panel(runtime);
            action(panel, "press_SHIFT"); action(panel, "release_SHIFT");
            runtime.start();
            await(() -> runtime.snapshot().lightningStepCooldown() > 0);
            assertTrue(runtime.snapshot().stamina() < 70);
            action(panel, "press_SHIFT"); // mantém após tentativa recusada em cooldown.
            await(() -> !runtime.snapshot().lightningTrails().isEmpty());
            action(panel, "restart");
            await(() -> runtime.snapshot().lightningStepCooldown() == 0);
            assertEquals(100, runtime.snapshot().stamina());
            assertFalse(runtime.snapshot().lightningStepActive());
            assertTrue(runtime.snapshot().lightningTrails().isEmpty());
            action(panel, "press_SHIFT"); action(panel, "press_SHIFT");
            long tick = runtime.snapshot().ticks();
            await(() -> runtime.snapshot().ticks() >= tick + 2);
            assertEquals(0, runtime.snapshot().lightningStepCooldown());
            action(panel, "release_SHIFT"); action(panel, "press_SHIFT");
            await(() -> runtime.snapshot().lightningStepCooldown() > 0);
        }
    }

    /** O mesmo reinício ordenado da fila descarta comandos de dash ainda pendentes. */
    @Test void reinicioDescartaDashPendente() {
        var buffer = new DebugCommandBuffer();
        buffer.cast(DASH); buffer.restart();
        var frame = buffer.drain();
        assertTrue(frame.restart()); assertTrue(frame.casts().isEmpty());
        assertTrue(buffer.drain().casts().isEmpty());
    }

    /** Monta os bindings reais e posiciona o mouse em ponto válido, sem JFrame. */
    private DebugPanel panel(DebugRuntime runtime) throws Exception {
        var panel = new DebugPanel(runtime::snapshot);
        SwingUtilities.invokeAndWait(() -> {
            DebugLauncher.configureKeys(panel, runtime, new HashSet<>());
            var event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, 500, 340, 0, false);
            for (var listener : panel.getMouseMotionListeners()) listener.mouseMoved(event);
        });
        return panel;
    }

    /** Executa uma ação da janela no EDT. */
    private void action(DebugPanel panel, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> panel.getActionMap().get(name)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name)));
    }

    /** Aguarda publicação de um estado sem acessar entidades fora da thread proprietária. */
    private void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        assertTrue(condition.getAsBoolean(), "Estado esperado em até 3 s");
    }
}
