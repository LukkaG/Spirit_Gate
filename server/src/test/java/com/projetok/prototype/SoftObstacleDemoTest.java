package com.projetok.prototype;

import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.factory.PlayerArchetype;
import com.projetok.domain.factory.PlayerFactory;
import com.projetok.domain.movement.CollisionCategory;
import com.projetok.domain.movement.CollisionPolicy;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.MovementService;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.resource.StaminaService;
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

import static org.junit.jupiter.api.Assertions.*;

/** Valida os objetos leves reais do demo sem abrir janelas nem alterar baselines históricos. */
class SoftObstacleDemoTest {
    private static final float DT = 1f / 30f;
    private static final float RADIUS = DebugWorld.PLAYER_RADIUS;
    private static final DebugInput DOWN = new DebugInput(false, true, false, false, false);
    private static final DebugInput RIGHT = new DebugInput(false, false, false, true, false);

    /** O cenário histórico continua com três sólidos; o demo acrescenta somente metadados leves. */
    @Test
    void separaDemoDoCenarioHistoricoEPublicaListaImutavel() {
        var original = new DebugWorld(new Random(8)).snapshot();
        var demo = DebugWorld.demo(new Random(8)).snapshot();
        assertEquals(original.obstacles(), demo.obstacles());
        assertEquals(3, original.obstacles().size());
        assertTrue(original.softObstacles().isEmpty());
        assertEquals(3, demo.softObstacles().size());
        assertEquals(2, demo.softObstacles().stream()
                .filter(o -> o.kind() == DebugSnapshot.SoftObstacleKind.BUSH).count());
        assertEquals(1, demo.softObstacles().stream()
                .filter(o -> o.kind() == DebugSnapshot.SoftObstacleKind.POST).count());
        assertThrows(UnsupportedOperationException.class, () -> demo.softObstacles().clear());
        for (var obstacle : geometry(demo)) assertEquals(CollisionCategory.SOFT_OBSTACLE, obstacle.category());
        for (var obstacle : original.obstacles()) {
            var body = new CollisionService.Obstacle(obstacle.x(), obstacle.y(), obstacle.width(), obstacle.height());
            assertEquals(CollisionCategory.SOLID_OBSTACLE, body.category());
        }
    }

    /** A arena aberta pelo launcher usa os objetos demonstrativos, sem iniciar a thread neste teste. */
    @Test
    void runtimeAbreOCenarioDemonstrativo() {
        try (var runtime = new DebugRuntime()) {
            assertEquals(3, runtime.snapshot().softObstacles().size());
            assertEquals(3, runtime.snapshot().obstacles().size());
        }
    }

    /** Exercita cada hitbox publicada com movimento comum, dash sólido e dash seletivo. */
    @Test
    void todosObjetosBloqueiamWasdEDashSolidoMasPermitemDashSeletivo() {
        for (var obstacle : geometry(DebugWorld.demo(new Random(8)).snapshot())) {
            var collisions = new CollisionService();
            float centerX = obstacle.x() + obstacle.width() / 2;
            float originY = obstacle.y() - 40;
            var walking = player(centerX, originY);
            var space = new CollisionService.Space(DebugWorld.WIDTH, DebugWorld.HEIGHT,
                    List.of(obstacle), List.of());
            var movement = new MovementService(collisions, MovementService.Response.SOFT_SLIDE);
            for (int i = 0; i < 20; i++) {
                movement.moveDirected(walking, new Position(0, 1), DebugWorld.WALK_SPEED, DT, RADIUS, space);
            }
            assertTrue(walking.getPosition().y() <= obstacle.y() - RADIUS);
            assertTrue(collisions.isFree(walking, walking.getPosition().x(), walking.getPosition().y(), RADIUS, space));

            var solid = player(centerX, originY);
            var solidDash = dash(collisions, CollisionPolicy.SOLID);
            execute(solidDash, solid, centerX, originY + 200, space);
            assertEquals(obstacle.y() - RADIUS, solid.getPosition().y(), 0.0001);
            assertFalse(solidDash.active());

            var selective = player(centerX, originY);
            var selectiveDash = dash(collisions, CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);
            assertEquals(LightningStepService.Status.STARTED, selectiveDash.cast(selective,
                    new SkillCastCommand(LightningStepService.ID, centerX, originY + 200), RADIUS, space));
            assertEquals(65, selective.require(StaminaComponent.class).current());
            for (int i = 0; i < 7; i++) selectiveDash.update(selective, DT, RADIUS, space);
            assertTrue(selectiveDash.active(), "O bloqueio não altera a duração da habilidade");
            selectiveDash.update(selective, DT, RADIUS, space);
            assertFalse(selectiveDash.active());
            assertFalse(selectiveDash.awaitingLanding());
            assertEquals(originY + 150, selective.getPosition().y(), 0.0001);
            assertEquals(4 - 8 * DT, selectiveDash.cooldown(), 0.0001);
            assertTrue(collisions.isFree(selective, centerX, selective.getPosition().y(), RADIUS, space));
        }
    }

    /** A física autoritativa bloqueia a caminhada e atravessa arbusto e poste no mesmo dash. */
    @Test
    void arenaDemonstraWasdBloqueadoTravessiaEPousoLivre() throws Exception {
        var world = DebugWorld.demo(new Random(8));
        for (int i = 0; i < 13; i++) world.tick(DOWN, DT);
        var before = world.snapshot();
        for (int i = 0; i < 3; i++) world.tick(RIGHT, DT);
        assertEquals(before.actors().get(0).x(), world.snapshot().actors().get(0).x());
        var origin = world.snapshot().actors().get(0);
        var mobHp = world.snapshot().actors().stream().skip(1).map(DebugSnapshot.ActorView::hp).toList();
        world.tickQueued(DebugInput.NONE, DT, 0,
                List.of(new SkillCastCommand(LightningStepService.ID, 500, origin.y())));
        assertEquals(65, world.snapshot().stamina());
        DebugSnapshot crossing = null;
        for (int i = 0; i < 8; i++) {
            world.tick(DebugInput.NONE, DT);
            var current = world.snapshot();
            if (i == 1) crossing = current;
            assertEquals(mobHp, current.actors().stream().skip(1).map(DebugSnapshot.ActorView::hp).toList());
        }
        var landed = world.snapshot();
        assertNotNull(crossing);
        assertTrue(crossing.lightningStepActive());
        assertTrue(crossing.actors().get(0).x() > 160 && crossing.actors().get(0).x() < 196,
                "O jogador cruza a hitbox publicada do segundo arbusto");
        assertEquals(origin.x() + 150, landed.actors().get(0).x(), 0.0001);
        assertEquals(origin.y(), landed.actors().get(0).y());
        assertFalse(landed.lightningStepActive());
        assertFalse(landed.lightningStepAwaitingLanding());
        assertTrue(landed.actors().get(0).x() - RADIUS >= 264, "Pousa depois do poste, fora da hitbox");
        capture(world, crossing, landed);
        assertEquals(landed, world.snapshot(), "O painel não altera o mundo ao renderizar");
        world.tick(RIGHT, DT);
        assertEquals(landed.actors().get(0).x() + DebugWorld.WALK_SPEED * DT,
                world.snapshot().actors().get(0).x(), 0.0001);
    }

    /** Um destino ocupado no layout real recua até o último ponto normal livre do caminho. */
    @Test
    void destinoDentroDoArbustoRecuaSemAtravessarOutroObstaculo() {
        var collisions = new CollisionService();
        var player = player(35, 440);
        var space = new CollisionService.Space(DebugWorld.WIDTH, DebugWorld.HEIGHT,
                geometry(DebugWorld.demo(new Random(8)).snapshot()), List.of());
        var service = dash(collisions, CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);
        execute(service, player, 500, 440, space);
        assertEquals(142, player.getPosition().x(), 0.0001);
        assertEquals(440, player.getPosition().y());
        assertTrue(collisions.isFree(player, 142, 440, RADIUS, space));
        assertFalse(service.awaitingLanding());
    }

    /** As paredes históricas continuam bloqueantes na mesma arena que contém arbustos e poste. */
    @Test
    void cenarioDemoPreservaColisaoComParedeSolida() {
        var world = DebugWorld.demo(new Random(8));
        for (int i = 0; i < 8; i++) world.tick(RIGHT, DT);
        world.tickQueued(DebugInput.NONE, DT, 0,
                List.of(new SkillCastCommand(LightningStepService.ID, 700, 340)));
        for (int i = 0; i < 8; i++) world.tick(DebugInput.NONE, DT);
        assertEquals(346 - RADIUS, world.snapshot().actors().get(0).x(), 0.0001);
        assertFalse(world.snapshot().lightningStepActive());
    }

    /** Reconstrói somente os valores publicados, para testar cada forma demonstrativa isoladamente. */
    private static List<CollisionService.Obstacle> geometry(DebugSnapshot snapshot) {
        return snapshot.softObstacles().stream().map(obstacle -> new CollisionService.Obstacle(
                obstacle.x(), obstacle.y(), obstacle.width(), obstacle.height(), CollisionCategory.SOFT_OBSTACLE)).toList();
    }

    /** Cria a habilidade com a física existente; a política não duplica geometria nem resolução. */
    private static LightningStepService dash(CollisionService collisions, CollisionPolicy policy) {
        return new LightningStepService(new MovementService(collisions), new StaminaService(), collisions, policy);
    }

    /** Avança o quarto de segundo em oito ticks, incluindo o último passo fracionário. */
    private static void execute(LightningStepService dash, Player player, float x, float y, CollisionService.Space space) {
        assertEquals(LightningStepService.Status.STARTED,
                dash.cast(player, new SkillCastCommand(LightningStepService.ID, x, y), RADIUS, space));
        for (int i = 0; i < 8; i++) dash.update(player, DT, RADIUS, space);
    }

    /** Usa a fábrica real com vida e vigor iniciais, sem rede, banco ou janela. */
    private static Player player(float x, float y) {
        var archetype = new PlayerArchetype(RaceRegistry.withDefaults().get("human"), 30, 8, 14, null, null);
        return new PlayerFactory(new VitalityService()).create(1, "Demo", archetype,
                new Position(x, y), new PlayerConnection() {
                    @Override public void send(Object packet) { /* apenas simulação local */ }
                    @Override public void disconnect() { /* nenhum socket */ }
                });
    }

    /** Registra dois quadros da renderização real com hitboxes: durante e depois da travessia. */
    private static void capture(DebugWorld world, DebugSnapshot crossing, DebugSnapshot landed) throws Exception {
        int header = 36;
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH * 2,
                DebugPanel.PANEL_HEIGHT + header, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(10, 21, 35));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
            graphics.setColor(new Color(181, 238, 132));
            graphics.drawString("DURANTE: dash atravessa o arbusto (H: hitboxes explícitas)", 18, 24);
            graphics.drawString("DEPOIS: pouso livre além do poste; WASD volta ao normal",
                    DebugPanel.PANEL_WIDTH + 18, 24);
            var panel = new DebugPanel(world::snapshot);
            panel.setShowHitboxes(true);
            paintFrame(panel, graphics, crossing, 0, header);
            paintFrame(panel, graphics, landed, DebugPanel.PANEL_WIDTH, header);
        } finally {
            graphics.dispose();
        }
        Path output = Path.of("target/task008-demo.png");
        Files.createDirectories(output.getParent());
        assertTrue(ImageIO.write(image, "png", output.toFile()));
    }

    /** Isola o estado gráfico de cada quadro para reproduzir o mesmo painel do launcher. */
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
