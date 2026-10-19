package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica entrada, vigor e preservação das mecânicas na simulação local. */
class StaminaWorldTest {
    private static final float DT = 1f / 30f;
    private static final DebugInput E = new DebugInput(false, false, false, false, false, true);

    /** A fábrica anexa o recurso e a entrada antiga continua sem esforço. */
    @Test
    void fabricaECompatibilidadeDaEntrada() {
        var world = new DebugWorld();
        assertEquals(100, world.snapshot().stamina());
        assertEquals(100, world.snapshot().maxStamina());
        assertFalse(new DebugInput(false, false, false, false, true).effort());
    }

    /** Manter a tecla não repete o gasto; soltar e pressionar habilita outro. */
    @Test
    void gastoUnicoPorPressao() {
        var world = new DebugWorld();
        world.tick(E, DT);
        assertEquals(80, world.snapshot().stamina());
        for (int i = 0; i < 10; i++) world.tick(E, DT);
        assertEquals(85, world.snapshot().stamina());
        world.tick(DebugInput.NONE, DT);
        world.tick(E, DT);
        assertEquals(66, world.snapshot().stamina());
    }

    /** Falta de saldo só regenera e mantém a ação rejeitada até nova pressão. */
    @Test
    void insuficienteNaoConsomeNemRepeteAutomaticamente() {
        var world = new DebugWorld();
        for (int i = 0; i < 5; i++) {
            world.tick(E, DT);
            world.tick(DebugInput.NONE, DT);
        }
        assertEquals(4.5f, world.snapshot().stamina());
        world.tick(E, DT);
        assertEquals(5, world.snapshot().stamina());
        assertTrue(world.snapshot().lastEvent().contains("Vigor insuficiente"));
        for (int i = 0; i < 40; i++) world.tick(E, DT);
        assertEquals(25, world.snapshot().stamina());
        world.tick(DebugInput.NONE, DT);
        world.tick(E, DT);
        assertEquals(6, world.snapshot().stamina());
    }

    /** Reinício restaura o recurso sem consumir por uma tecla já mantida. */
    @Test
    void reinicioRestauraVigorEExigeNovaPressao() {
        var old = new DebugWorld();
        old.tick(E, DT);
        var restarted = new DebugWorld();
        restarted.synchronizeEffort(true);
        restarted.tick(E, DT);
        assertEquals(100, restarted.snapshot().stamina());
        restarted.tick(DebugInput.NONE, DT);
        restarted.tick(E, DT);
        assertEquals(80, restarted.snapshot().stamina());
        assertEquals(80, old.snapshot().stamina());
    }

    /** Sem vigor, ataque básico e movimento continuam ativos com as mesmas regras. */
    @Test
    void ataqueEMovimentoIndependemDoVigor() {
        var world = new DebugWorld();
        for (int i = 0; i < 5; i++) {
            world.tick(E, DT);
            world.tick(DebugInput.NONE, DT);
        }
        var before = world.snapshot();
        world.tick(new DebugInput(false, false, false, true, true), DT);
        var after = world.snapshot();
        assertEquals(before.stamina() + 0.5f, after.stamina());
        assertNotNull(after.attack());
        assertEquals(1, after.attack().facingX());
        assertEquals(0.38f, after.cooldown());
        assertEquals(before.actors().get(0).x() + DebugWorld.WALK_SPEED * DT,
                after.actors().get(0).x());
    }

    /** Esforço físico não causa dano; ataque básico continua causando 15 com vigor baixo. */
    @Test
    void danoDoAtaqueBasicoPreservadoComVigorBaixo() {
        var world = new DebugWorld();
        for (int i = 0; i < 5; i++) {
            world.tick(E, DT);
            world.tick(DebugInput.NONE, DT);
        }
        assertEquals(120, world.snapshot().actors().get(1).hp());
        boolean hit = false;
        for (int i = 0; i < 100; i++) {
            var before = world.snapshot();
            var p = before.actors().get(0);
            var mob = before.actors().get(1);
            float dx = mob.x() - p.x(), dy = mob.y() - p.y();
            world.tick(new DebugInput(dy < -4, dy > 4, dx < -4, dx > 4, true), DT);
            if (world.snapshot().actors().get(1).hp() < 120) {
                assertEquals(105, world.snapshot().actors().get(1).hp());
                assertTrue(world.snapshot().stamina() < 20);
                hit = true;
                break;
            }
        }
        assertTrue(hit);
    }

    /** Após morrer, nem esforço, movimento, ataque ou regeneração são processados. */
    @Test
    void morteBloqueiaAcoesERegeneracao() {
        var world = new DebugWorld();
        for (int i = 0; i < 2000 && !world.snapshot().gameOver(); i++) {
            // Gasta vigor perto da morte para detectar regeneração indevida após morrer.
            boolean effort = world.snapshot().playerHp() <= 20 && i % 2 == 0;
            world.tick(new DebugInput(false, false, false, false, false, effort), DT);
        }
        assertTrue(world.snapshot().gameOver());
        var before = world.snapshot();
        assertTrue(before.stamina() < 100);
        for (int i = 0; i < 20; i++)
            world.tick(new DebugInput(false, false, false, true, true, i % 2 == 0), DT);
        var after = world.snapshot();
        assertEquals(before.stamina(), after.stamina());
        assertEquals(before.actors().get(0), after.actors().get(0));
        assertNull(after.attack());
        assertEquals(before.lastEvent(), after.lastEvent());
    }

    /** Delta não finito é recusado antes de alterar tick, vigor ou posição. */
    @Test
    void rejeitaDeltaNaoFinitoAntesDoTick() {
        var world = new DebugWorld();
        var before = world.snapshot();
        for (float dt : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> world.tick(E, dt));
        assertEquals(before, world.snapshot());
    }
}
