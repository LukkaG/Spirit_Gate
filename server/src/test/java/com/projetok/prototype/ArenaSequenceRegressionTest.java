package com.projetok.prototype;

import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.LightningStepService;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.prototype.DebugSnapshot.SkillFeedbackKind;
import com.projetok.prototype.DebugSnapshot.SkillState;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/** Sequências do runtime e dos controles reais, sem agendador, espera ou JFrame. */
class ArenaSequenceRegressionTest {
    private static final float DT = 1f / 30f;

    // --- Entrada real, FIFO e duração discreta ---

    /** Toques entre ticks chegam à fila; autorepeat não reativa após a recarga terminar. */
    @Test
    void shiftRapidoEMantidoNaoRepeteMesmoDepoisDoCooldown() throws Exception {
        try (var runtime = runtime()) {
            var panel = panel(runtime);
            aim(panel, 500, 340);
            action(panel, "press_SHIFT", "release_SHIFT");
            runtime.advanceOneTick();
            var started = runtime.snapshot();
            assertTrue(started.lightningStepActive());
            assertEquals(65, started.stamina());

            // Esta pressão é recusada por cooldown e continua mantida até a recarga acabar.
            action(panel, "press_SHIFT", "press_SHIFT", "press_SHIFT");
            runtime.advanceOneTick();
            assertEquals(SkillFeedbackKind.COOLDOWN, runtime.snapshot().lightningStepHud().feedback().kind());
            assertEquals(65.5f, runtime.snapshot().stamina());
            advance(runtime, 121);
            var ready = runtime.snapshot();
            assertFalse(ready.gameOver());
            assertEquals(0, ready.lightningStepCooldown());
            assertFalse(ready.lightningStepActive());
            assertEquals(100, ready.stamina());
            action(panel, "press_SHIFT", "press_SHIFT");
            runtime.advanceOneTick();
            assertFalse(runtime.snapshot().lightningStepActive());
            assertEquals(100, runtime.snapshot().stamina());

            action(panel, "release_SHIFT", "press_SHIFT", "release_SHIFT");
            runtime.advanceOneTick();
            assertTrue(runtime.snapshot().lightningStepActive());
            assertEquals(65, runtime.snapshot().stamina());
            assertEquals(4, runtime.snapshot().lightningStepCooldown());
        }
    }

    /** A ordem efetiva dos bindings decide qual habilidade recebe o saldo compartilhado. */
    @Test
    void qEShiftCompetemEmFifoComVigorInsuficienteParaAmbas() throws Exception {
        for (boolean cryoFirst : new boolean[] {true, false}) {
            try (var runtime = runtime()) {
                var panel = panel(runtime);
                action(panel, "press_E", "release_E");
                runtime.advanceOneTick();
                assertEquals(80, runtime.snapshot().stamina());
                aim(panel, 500, 340);
                action(panel, cryoFirst ? "press_Q" : "press_SHIFT",
                        cryoFirst ? "press_SHIFT" : "press_Q");
                runtime.advanceOneTick();
                var result = runtime.snapshot();
                assertEquals(cryoFirst ? 25.5f : 45.5f, result.stamina());
                assertEquals(cryoFirst, !result.iceZones().isEmpty());
                assertEquals(!cryoFirst, result.lightningStepActive());
                assertEquals(cryoFirst ? 3 : 0, result.cryostasisCooldown());
                assertEquals(cryoFirst ? 0 : 4, result.lightningStepCooldown());
                assertEquals(cryoFirst ? SkillFeedbackKind.ACCEPTED : SkillFeedbackKind.INSUFFICIENT_STAMINA,
                        result.cryostasisHud().feedback().kind());
                assertEquals(cryoFirst ? SkillFeedbackKind.INSUFFICIENT_STAMINA : SkillFeedbackKind.ACCEPTED,
                        result.lightningStepHud().feedback().kind());
            }
        }
    }

    /** O lote suficiente aceita ambas; novas pressões durante cooldown só regeneram vigor. */
    @Test
    void qEShiftNoMesmoTickConsomemUmaVezERecusasNaoCobramNovamente() throws Exception {
        try (var runtime = runtime()) {
            var panel = panel(runtime);
            aim(panel, 500, 340);
            action(panel, "press_Q", "release_Q", "press_SHIFT", "release_SHIFT");
            runtime.advanceOneTick();
            assertEquals(10, runtime.snapshot().stamina());
            assertEquals(1, runtime.snapshot().iceZones().size());
            assertTrue(runtime.snapshot().lightningStepActive());
            assertEquals(SkillFeedbackKind.ACCEPTED, runtime.snapshot().cryostasisHud().feedback().kind());
            assertEquals(SkillFeedbackKind.ACCEPTED, runtime.snapshot().lightningStepHud().feedback().kind());
            action(panel, "press_SHIFT", "release_SHIFT", "press_Q", "release_Q");
            runtime.advanceOneTick();
            assertEquals(10.5f, runtime.snapshot().stamina());
            assertEquals(1, runtime.snapshot().iceZones().size());
            assertEquals(SkillFeedbackKind.COOLDOWN, runtime.snapshot().cryostasisHud().feedback().kind());
            assertEquals(SkillFeedbackKind.COOLDOWN, runtime.snapshot().lightningStepHud().feedback().kind());
            assertEquals(3 - DT, runtime.snapshot().cryostasisCooldown(), 0.00001);
            assertEquals(4 - DT, runtime.snapshot().lightningStepCooldown(), 0.00001);
        }
    }

    /** A troca de direção durante o dash espera o último tick fracionário; ataque continua disponível. */
    @Test
    void wasdEAtaqueAtravessamInicioFimDoDashSemSomarDeslocamento() throws Exception {
        try (var runtime = runtime()) {
            var panel = panel(runtime);
            aim(panel, 500, 340);
            action(panel, "press_D", "press_SHIFT", "release_SHIFT");
            runtime.advanceOneTick();
            var origin = runtime.snapshot().actors().get(0);
            assertEquals(135 + DebugWorld.WALK_SPEED * DT, origin.x(), 0.0001);
            assertEquals(340, origin.y());
            action(panel, "release_D", "press_W", "press_SPACE");
            runtime.advanceOneTick();
            assertNotNull(runtime.snapshot().attack());
            assertEquals(65.5f, runtime.snapshot().stamina());
            assertEquals(origin.y(), runtime.snapshot().actors().get(0).y());
            action(panel, "release_SPACE");
            advance(runtime, 7);
            var completed = runtime.snapshot();
            assertFalse(completed.lightningStepActive());
            assertEquals(origin.x() + 150, completed.actors().get(0).x(), 0.0001);
            assertEquals(origin.y(), completed.actors().get(0).y());
            runtime.advanceOneTick();
            var resumed = runtime.snapshot();
            assertEquals(completed.actors().get(0).x(), resumed.actors().get(0).x());
            assertEquals(origin.y() - DebugWorld.WALK_SPEED * DT, resumed.actors().get(0).y(), 0.0001);
            assertEquals(0, resumed.facingX());
            assertEquals(-1, resumed.facingY());
        }
    }

    // --- Efeitos, morte e reinício no mesmo runtime ---

    /** Um Freeze renovado pelos pulsos trava a perseguição, mas preserva o próximo ataque da IA. */
    @Test
    void freezeDaFilaImpedeMovimentoMasPermiteAtaqueDoMob() {
        try (var runtime = runtime()) {
            for (int tick = 0; tick < 200 && runtime.snapshot().playerHp() == 100; tick++)
                runtime.advanceOneTick();
            assertTrue(runtime.snapshot().playerHp() < 100);
            var mob = runtime.snapshot().actors().get(1);
            runtime.cast(new SkillCastCommand(CryostasisService.ID, mob.x(), mob.y()));
            runtime.advanceOneTick();
            var frozen = runtime.snapshot();
            assertEquals(1, frozen.statuses().size());
            assertEquals(mob.id(), frozen.statuses().get(0).actorId());
            assertEquals(1, frozen.statuses().get(0).freezeRemaining());
            boolean attacked = false;
            for (int tick = 0; tick < 45; tick++) {
                runtime.advanceOneTick();
                var current = runtime.snapshot();
                assertFalse(current.statuses().isEmpty());
                assertEquals(mob.x(), current.actors().get(1).x());
                assertEquals(mob.y(), current.actors().get(1).y());
                if (current.playerHp() < frozen.playerHp()) attacked = true;
            }
            assertTrue(attacked, "O cooldown da IA termina sem exigir remoção do Freeze");
            assertTrue(runtime.snapshot().actors().get(1).hp() < frozen.actors().get(1).hp(),
                    "Os pulsos de dano continuam durante o congelamento");
        }
    }

    /** R recria o demo, descarta ações anteriores e não reativa Shift/E mantidos. */
    @Test
    void reinicioRealLimpaAtivosStatusRastrosHudEFilaSemReativarTeclasMantidas() throws Exception {
        try (var runtime = runtime()) {
            var panel = panel(runtime);
            var mob = runtime.snapshot().actors().get(1);
            aim(panel, (int) mob.x(), (int) mob.y());
            action(panel, "press_Q", "release_Q");
            aim(panel, 500, 340);
            action(panel, "press_SHIFT");
            runtime.advanceOneTick();
            runtime.advanceOneTick();
            var old = runtime.snapshot();
            assertTrue(old.lightningStepActive());
            assertFalse(old.lightningTrails().isEmpty());
            assertFalse(old.iceZones().isEmpty());
            assertFalse(old.statuses().isEmpty());

            // Estes gastos pendentes são anteriores a R e devem desaparecer da nova arena.
            action(panel, "press_E", "press_Q", "restart", "press_SHIFT");
            runtime.advanceOneTick();
            var restarted = runtime.snapshot();
            assertEquals(1, restarted.ticks());
            assertEquals(100, restarted.stamina());
            assertEquals(100, restarted.playerHp());
            assertEquals(0, restarted.cryostasisCooldown());
            assertEquals(0, restarted.lightningStepCooldown());
            assertFalse(restarted.lightningStepActive());
            assertFalse(restarted.lightningStepAwaitingLanding());
            assertTrue(restarted.lightningTrails().isEmpty());
            assertTrue(restarted.iceZones().isEmpty());
            assertTrue(restarted.statuses().isEmpty());
            assertEquals(SkillState.READY, restarted.cryostasisHud().state());
            assertEquals(SkillState.READY, restarted.lightningStepHud().state());
            assertEquals(SkillFeedbackKind.NONE, restarted.cryostasisHud().feedback().kind());
            assertEquals(SkillFeedbackKind.NONE, restarted.lightningStepHud().feedback().kind());
            assertEquals(old.softObstacles(), restarted.softObstacles());
            assertEquals(3, restarted.softObstacles().size());

            action(panel, "press_SHIFT", "press_SHIFT", "press_E");
            advance(runtime, 3);
            assertEquals(100, runtime.snapshot().stamina());
            assertFalse(runtime.snapshot().lightningStepActive());
            action(panel, "release_E", "release_Q", "release_SHIFT", "press_SHIFT", "release_SHIFT");
            runtime.advanceOneTick();
            assertTrue(runtime.snapshot().lightningStepActive());
            assertEquals(65, runtime.snapshot().stamina());
        }
    }

    /** Após morrer, lotes completos não movem, atacam ou regeneram; R restaura a mesma arena demo. */
    @Test
    void morteRecusaLoteDeAcoesEReinicioRestauraRecursosEDisponibilidade() throws Exception {
        try (var runtime = runtime()) {
            var panel = panel(runtime);
            for (int tick = 0; tick < 1800 && runtime.snapshot().playerHp() > 20; tick++)
                runtime.advanceOneTick();
            assertFalse(runtime.snapshot().gameOver());
            assertTrue(runtime.snapshot().playerHp() <= 20);
            for (int press = 0; press < 5; press++) action(panel, "press_E", "release_E");
            runtime.advanceOneTick();
            assertEquals(0, runtime.snapshot().stamina());
            for (int tick = 0; tick < 150 && !runtime.snapshot().gameOver(); tick++)
                runtime.advanceOneTick();
            var dead = runtime.snapshot();
            assertTrue(dead.gameOver());
            assertTrue(dead.stamina() < 100);

            aim(panel, 500, 340);
            action(panel, "press_D", "press_SPACE", "press_E", "press_Q", "press_SHIFT");
            advance(runtime, 20);
            var refused = runtime.snapshot();
            assertEquals(dead.actors().get(0), refused.actors().get(0));
            assertEquals(dead.stamina(), refused.stamina());
            assertEquals(0, refused.cryostasisCooldown());
            assertEquals(0, refused.lightningStepCooldown());
            assertTrue(refused.iceZones().isEmpty());
            assertFalse(refused.lightningStepActive());
            assertNull(refused.attack());
            assertEquals(SkillState.DEAD, refused.cryostasisHud().state());
            assertEquals(SkillState.DEAD, refused.lightningStepHud().state());

            action(panel, "release_D", "release_SPACE", "release_E", "release_Q", "release_SHIFT", "restart");
            runtime.advanceOneTick();
            var restarted = runtime.snapshot();
            assertFalse(restarted.gameOver());
            assertEquals(100, restarted.playerHp());
            assertEquals(100, restarted.stamina());
            assertEquals(SkillState.READY, restarted.cryostasisHud().state());
            assertEquals(SkillState.READY, restarted.lightningStepHud().state());
            assertEquals(3, restarted.softObstacles().size());
        }
    }

    /** A fábrica reinicia também a sequência aleatória; estados completos repetem sem hash novo. */
    @Test
    void sequenciaCompletaEReinicioSaoReproduziveisComSorteiosControlados() {
        assertEquals(replay(), replay());
    }

    // --- Fixtures: somente portas existentes da simulação e da entrada ---

    /** Cria o demo verdadeiro; cada reinício possui um gerador próprio com amostra 0,29. */
    private static DebugRuntime runtime() {
        return new DebugRuntime(() -> DebugWorld.demo(new RandomGenerator() {
            @Override public long nextLong() { return 0; }
            @Override public double nextDouble() { return 0.29; }
        }));
    }

    /** Monta o controlador real no EDT sem janela nem laço agendado. */
    private static DebugPanel panel(DebugRuntime runtime) throws Exception {
        var panel = new DebugPanel(runtime::snapshot);
        SwingUtilities.invokeAndWait(() -> DebugLauncher.configureKeys(panel, runtime, new HashSet<>()));
        return panel;
    }

    /** Captura o alvo pelos mesmos listeners de mouse usados pela arena. */
    private static void aim(DebugPanel panel, int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var event = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false);
            for (var listener : panel.getMouseMotionListeners()) listener.mouseMoved(event);
        });
    }

    /** Entrega um lote de ações no EDT antes de qualquer tick da simulação. */
    private static void action(DebugPanel panel, String... names) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String name : names) panel.getActionMap().get(name)
                    .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, name));
        });
    }

    /** Avança passos fixos da produção; não inicia a thread de agendamento. */
    private static void advance(DebugRuntime runtime, int ticks) {
        for (int tick = 0; tick < ticks; tick++) runtime.advanceOneTick();
    }

    /** Registra uma sequência inteira por valores, preservando os baselines históricos. */
    private static List<DebugSnapshot> replay() {
        List<DebugSnapshot> states = new ArrayList<>();
        try (var runtime = runtime()) {
            for (int round = 0; round < 2; round++) {
                if (round > 0) {
                    runtime.restart();
                    runtime.advanceOneTick();
                }
                var mob = runtime.snapshot().actors().get(1);
                runtime.cast(new SkillCastCommand(CryostasisService.ID, mob.x(), mob.y()));
                runtime.cast(new SkillCastCommand(LightningStepService.ID, 500, 340));
                runtime.setInput(new DebugInput(true, false, false, false, false));
                for (int tick = 0; tick < 50; tick++) {
                    runtime.advanceOneTick();
                    states.add(runtime.snapshot());
                }
                runtime.setInput(DebugInput.NONE);
            }
        }
        return List.copyOf(states);
    }
}
