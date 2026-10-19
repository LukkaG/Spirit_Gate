package com.projetok.prototype;

import com.projetok.domain.skill.LightningStepService;
import com.projetok.domain.skill.SkillCastCommand;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercita a arena real e registra o cruzamento de um ator sem aplicar dano de dash. */
class SelectiveDashIntegrationTest {
    private static final float DT = 1f / 30f;

    /** Comprova a passagem, o pouso livre e o dano recebido sem modificar a simulação ao desenhar. */
    @Test
    void atravessaMobNaArenaSemCausarDanoEPublicaCaptura() throws Exception {
        var world = new DebugWorld(new Random(61));
        for (int i = 0; i < 40; i++) world.tick(DebugInput.NONE, DT);
        var before = world.snapshot();
        var origin = before.actors().get(0);
        var mob = before.actors().get(1);
        int playerHp = before.playerHp();

        // O alvo é o centro atual do mob; a distância total do dash permanece 150 px.
        world.tickQueued(DebugInput.NONE, DT, 0,
                List.of(new SkillCastCommand(LightningStepService.ID, mob.x(), mob.y())));
        assertTrue(world.snapshot().lightningStepActive());
        assertEquals(65, world.snapshot().stamina());
        DebugSnapshot crossing = null;
        for (int i = 0; i < 8; i++) {
            world.tick(DebugInput.NONE, DT);
            var current = world.snapshot();
            if (distance(current.actors().get(0), current.actors().get(1))
                    < DebugWorld.PLAYER_RADIUS + DebugWorld.MOB_RADIUS) {
                assertTrue(current.lightningStepActive(), "Sobreposição é permitida durante a travessia");
                crossing = current;
            }
            for (int actor = 1; actor < current.actors().size(); actor++) {
                assertEquals(before.actors().get(actor).hp(), current.actors().get(actor).hp(),
                        "A passagem não aplica dano aos mobs");
            }
        }

        var landed = world.snapshot();
        assertNotNull(crossing, "A captura deve demonstrar interseção real das hitboxes");
        assertFalse(landed.lightningStepActive());
        assertFalse(landed.lightningStepAwaitingLanding());
        assertEquals(150, distance(origin, landed.actors().get(0)), 0.001);
        for (int actor = 1; actor < landed.actors().size(); actor++) {
            assertTrue(distance(landed.actors().get(0), landed.actors().get(actor))
                    >= DebugWorld.PLAYER_RADIUS + DebugWorld.MOB_RADIUS,
                    "O dash termina fora dos corpos atuais");
        }
        assertTrue(landed.playerHp() < playerHp, "A travessia não concede invulnerabilidade");
        assertFalse(crossing.lightningTrails().isEmpty());
        assertFalse(landed.lightningTrails().isEmpty());
        capture(world, crossing, landed);
        assertEquals(landed, world.snapshot(), "Renderizar não altera o mundo autoritativo");
    }

    /** Mede as posições publicadas sem consultar entidades mutáveis do domínio. */
    private static double distance(DebugSnapshot.ActorView first, DebugSnapshot.ActorView second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    /** Produz dois quadros da renderização real com legendas e hitboxes visíveis. */
    private static void capture(DebugWorld world, DebugSnapshot crossing, DebugSnapshot landed) throws Exception {
        int header = 38;
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH * 2,
                DebugPanel.PANEL_HEIGHT + header, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(10, 21, 35));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
            graphics.setColor(new Color(255, 235, 110));
            graphics.drawString("DURANTE: atravessa o mob; hitboxes sobrepostas", 18, 25);
            graphics.drawString("DEPOIS: termina livre; mob sem dano; jogador pode receber dano",
                    DebugPanel.PANEL_WIDTH + 18, 25);
            var panel = new DebugPanel(world::snapshot);
            panel.setShowHitboxes(true);
            paintFrame(panel, graphics, crossing, 0, header);
            paintFrame(panel, graphics, landed, DebugPanel.PANEL_WIDTH, header);
        } finally {
            graphics.dispose();
        }
        Path output = Path.of("target/task006-1-colisao-seletiva.png");
        Files.createDirectories(output.getParent());
        assertTrue(ImageIO.write(image, "png", output.toFile()));
    }

    /** Isola a transformação de cada quadro para preservar o desenho original do painel. */
    private static void paintFrame(DebugPanel panel, Graphics2D original, DebugSnapshot snapshot, int x, int y) {
        Graphics2D graphics = (Graphics2D) original.create();
        try {
            graphics.translate(x, y);
            graphics.clipRect(0, 0, DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT);
            panel.render(graphics, snapshot);
        } finally {
            graphics.dispose();
        }
    }
}
