package com.projetok.prototype;

import com.projetok.domain.skill.SkillCastCommand;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.random.RandomGenerator;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Ordem autoritativa do Freeze, ataque e publicação de estado visual. */
class FreezeWorldTest {
    private static final float DT = 1f / 30f;

    /** Gerador controlado: depois da sequência, todas as tentativas falham. */
    private RandomGenerator random(double... samples) {
        return new RandomGenerator() {
            private int next;
            @Override public long nextLong() { return 0; }
            @Override public double nextDouble() { return next < samples.length ? samples[next++] : 0.99; }
        };
    }

    /** Lança no primeiro mob, dentro do alcance original. */
    private void cast(DebugWorld world) {
        var mob = world.snapshot().actors().get(1);
        world.tickQueued(DebugInput.NONE, DT, 0,
                List.of(new SkillCastCommand("cryostasis", mob.x(), mob.y())));
    }

    /** Freeze recém-aplicado não perde o delta do tick e já impede o movimento. */
    @Test void duracaoCompletaEExpiracaoNoTick() {
        var world = new DebugWorld(random(0.29));
        var origin = world.snapshot().actors().get(1);
        cast(world);
        var initial = world.snapshot();
        assertEquals(1f, initial.statuses().get(0).freezeRemaining());
        assertEquals(origin.x(), initial.actors().get(1).x());
        assertEquals(origin.y(), initial.actors().get(1).y());
        for (int i = 0; i < 29; i++) world.tick(DebugInput.NONE, DT);
        assertEquals(origin.x(), world.snapshot().actors().get(1).x());
        assertTrue(world.snapshot().statuses().get(0).freezeRemaining() > 0);
        world.tick(DebugInput.NONE, DT);
        assertTrue(world.snapshot().statuses().isEmpty());
        assertNotEquals(origin.y(), world.snapshot().actors().get(1).y());
        assertEquals(1f, initial.statuses().get(0).freezeRemaining());
        assertThrows(UnsupportedOperationException.class, () -> initial.statuses().clear());
    }

    /** Sucesso no pulso seguinte renova para um segundo, sem somar o tempo anterior. */
    @Test void renovacaoEntrePulsosSemAcumular() {
        var world = new DebugWorld(random(0.29, 0.29));
        cast(world);
        for (int i = 0; i < 15; i++) world.tick(DebugInput.NONE, DT);
        assertEquals(1f, world.snapshot().statuses().get(0).freezeRemaining());
        for (int i = 0; i < 15; i++) world.tick(DebugInput.NONE, DT);
        assertEquals(0.5f, world.snapshot().statuses().get(0).freezeRemaining(), 0.00001);
        for (int i = 0; i < 15; i++) world.tick(DebugInput.NONE, DT);
        assertTrue(world.snapshot().statuses().isEmpty());
    }

    /** Fracasso em 0,30 não congela; dano e caminhada permanecem ativos. */
    @Test void limiteTrintaPorCentoNaoCongela() {
        var world = new DebugWorld(random(0.30));
        var before = world.snapshot().actors().get(1);
        cast(world);
        assertTrue(world.snapshot().statuses().isEmpty());
        assertEquals(113, world.snapshot().actors().get(1).hp());
        assertNotEquals(before.y(), world.snapshot().actors().get(1).y());
    }

    /** Um mob próximo continua atacando mesmo com a movimentação bloqueada. */
    @Test void mobCongeladoAindaAtacaNoAlcance() {
        var world = new DebugWorld(random(0.29, 0.29, 0.29, 0.29, 0.29));
        for (int i = 0; i < 150 && world.snapshot().playerHp() == 100; i++) world.tick(DebugInput.NONE, DT);
        assertTrue(world.snapshot().playerHp() < 100);
        cast(world);
        var snapshot = world.snapshot();
        assertFalse(snapshot.statuses().isEmpty());
        int hp = snapshot.playerHp();
        var position = snapshot.actors().get(1);
        boolean attackedWhileFrozen = false;
        for (int i = 0; i < 45; i++) {
            world.tick(DebugInput.NONE, DT);
            if (!world.snapshot().statuses().isEmpty() && world.snapshot().playerHp() < hp) {
                assertEquals(position.x(), world.snapshot().actors().get(1).x());
                assertEquals(position.y(), world.snapshot().actors().get(1).y());
                attackedWhileFrozen = true;
                break;
            }
        }
        assertTrue(attackedWhileFrozen);
    }

    /** Mundo novo não reaproveita efeitos e jogador não aparece na lista de Freeze. */
    @Test void reinicioLimpaStatusSemCongelarJogador() {
        var world = new DebugWorld(random(0.29)); cast(world);
        assertEquals(100, world.snapshot().statuses().get(0).actorId());
        var restarted = new DebugWorld(random());
        assertTrue(restarted.snapshot().statuses().isEmpty());
        assertTrue(restarted.snapshot().iceZones().isEmpty());
    }

    /** Captura determinística de um mob congelado sem alterar o snapshot no painel. */
    @Test void capturaDoCongelamento() throws Exception {
        var world = new DebugWorld(random(0.29)); cast(world);
        var snapshot = world.snapshot();
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { new DebugPanel(world::snapshot).render(graphics, snapshot); }
        finally { graphics.dispose(); }
        assertEquals(snapshot, world.snapshot());
        assertFalse(snapshot.statuses().isEmpty());
        Files.createDirectories(Path.of("target"));
        ImageIO.write(image, "png", Path.of("target/task005-freeze.png").toFile());
    }
}
