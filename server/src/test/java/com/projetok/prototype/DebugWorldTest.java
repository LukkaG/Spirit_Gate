package com.projetok.prototype;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Testes JUnit 5 de regras visíveis do protótipo, sem abrir janela ou banco. */
class DebugWorldTest {
    private static final float TICK = 1f / 30f;

    /** Valida o reuso de PlayerFactory/MobFactory e os HPs raciais derivados. */
    @Test
    void criaJogadorETresInimigosComHpDerivado() {
        DebugSnapshot s = new DebugWorld().snapshot();
        assertEquals(4, s.actors().size());
        assertEquals(100, s.playerHp());
        assertEquals(120, s.actors().get(1).maxHp());
    }

    /** Movimento diagonal usa vetor normalizado, nunca gera velocidade extra. */
    @Test
    void normalizaVelocidadeDiagonal() {
        DebugWorld straight = new DebugWorld();
        DebugWorld diagonal = new DebugWorld();
        straight.tick(new DebugInput(false, false, false, true, false), TICK);
        diagonal.tick(new DebugInput(true, false, false, true, false), TICK);
        var a = straight.snapshot().actors().get(0);
        var b = diagonal.snapshot().actors().get(0);
        assertEquals(a.x() - 135f, Math.hypot(b.x() - 135f, b.y() - 340f), 0.01f);
    }

    /** Colisão mantém o centro do jogador fora de retângulos sólidos. */
    @Test
    void naoAtravessaObstaculos() {
        DebugWorld world = new DebugWorld();
        for (int n = 0; n < 180; n++) {
            world.tick(new DebugInput(false, false, false, true, false), TICK);
            DebugSnapshot s = world.snapshot();
            var p = s.actors().get(0);
            for (var o : s.obstacles()) {
                float closestX = Math.max(o.x(), Math.min(o.x() + o.width(), p.x()));
                float closestY = Math.max(o.y(), Math.min(o.y() + o.height(), p.y()));
                assertTrue(Math.hypot(p.x() - closestX, p.y() - closestY)
                        >= DebugWorld.PLAYER_RADIUS - .01f);
            }
        }
    }

    /** IA se aproxima de um alvo e utiliza o sistema de dano do dominio. */
    @Test
    void mobsCausamDanoAoJogador() {
        DebugWorld world = new DebugWorld();
        for (int i = 0; i < 210 && world.snapshot().playerHp() == 100; i++)
            world.tick(DebugInput.NONE, TICK);
        assertTrue(world.snapshot().playerHp() < 100);
    }

    /** Ataca com dano derivado do combate existente, aplicando cooldown de golpe. */
    @Test
    void aplicaDanoRealEImpedeAtaquesSemCooldown() {
        DebugWorld world = new DebugWorld();
        DebugSnapshot before = world.snapshot();
        boolean hit = false;
        for (int n = 0; n < 360; n++) {
            var p = before.actors().get(0);
            var mob = before.actors().get(1);
            float dx = mob.x() - p.x(), dy = mob.y() - p.y();
            world.tick(new DebugInput(dy < -4, dy > 4, dx < -4, dx > 4, true), TICK);
            DebugSnapshot after = world.snapshot();
            if (after.actors().get(1).hp() < 120) {
                assertEquals(105, after.actors().get(1).hp());
                world.tick(new DebugInput(false, false, false, false, true), TICK);
                assertEquals(105, world.snapshot().actors().get(1).hp());
                hit = true;
                break;
            }
            before = after;
        }
        assertTrue(hit, "Deve existir um caminho real de ataque até o mob");
    }

    /** Alcance e cone continuam impedindo dano mesmo quando há animação. */
    @Test
    void preservaAlcanceEConeDoAtaque() {
        DebugWorld distant = new DebugWorld();
        distant.tick(new DebugInput(false, false, false, false, true), TICK);
        assertNotNull(distant.snapshot().attack());
        assertEquals(120, distant.snapshot().actors().get(1).hp());

        DebugWorld world = new DebugWorld();
        for (int i = 0; i < 150; i++) {
            var s = world.snapshot();
            var p = s.actors().get(0);
            var m = s.actors().get(1);
            if (Math.hypot(m.x() - p.x(), m.y() - p.y()) <= 80) break;
            world.tick(DebugInput.NONE, TICK);
        }
        var before = world.snapshot();
        var p = before.actors().get(0);
        var m = before.actors().get(1);
        assertTrue(Math.hypot(m.x() - p.x(), m.y() - p.y()) <= 80);
        world.tick(new DebugInput(false, true, false, false, true), TICK);
        assertEquals(120, world.snapshot().actors().get(1).hp());
        assertEquals(1f, world.snapshot().attack().facingY());
    }
    @Test
    void corteNoArRespeitaCooldownDirecaoEExpiracao() {
        DebugWorld world = new DebugWorld();
        assertNull(world.snapshot().attack());
        world.tick(new DebugInput(true, false, false, true, true), TICK);
        var first = world.snapshot().attack();
        assertNotNull(first);
        assertEquals(Math.sqrt(0.5), first.facingX(), 0.0001);
        assertEquals(-Math.sqrt(0.5), first.facingY(), 0.0001);
        assertTrue(world.snapshot().effects().isEmpty());
        world.tick(new DebugInput(false, true, true, false, true), TICK);
        var next = world.snapshot().attack();
        assertEquals(first.facingX(), next.facingX());
        assertEquals(first.x(), next.x());
        assertTrue(next.remaining() < first.remaining());
        for (int i = 0; i < 5; i++) world.tick(DebugInput.NONE, TICK);
        assertNull(world.snapshot().attack());
        for (int i = 0; i < 6; i++) world.tick(DebugInput.NONE, TICK);
        world.tick(new DebugInput(false, false, false, false, true), TICK);
        assertNotNull(world.snapshot().attack());
        assertTrue(world.snapshot().attack().facingX() < 0);
    }

    @Test
    void renderizaCorteEmTodasAsDirecoesSemJanela() {
        boolean[][] directions = {{true,false,false,false}, {false,true,false,false},
                {false,false,true,false}, {false,false,false,true}, {true,false,false,true}};
        for (boolean[] d : directions) {
            DebugWorld world = new DebugWorld();
            world.tick(new DebugInput(d[0], d[1], d[2], d[3], true), TICK);
            var image = new java.awt.image.BufferedImage(DebugPanel.PANEL_WIDTH,
                    DebugPanel.PANEL_HEIGHT, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var g = image.createGraphics();
            try {
                new DebugPanel(world::snapshot).render(g, world.snapshot());
            } finally { g.dispose(); }
        }
    }
}
