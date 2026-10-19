package com.projetok.prototype;

import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
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
import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.LightningStepService;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.prototype.DebugSnapshot.SkillFeedbackKind;
import com.projetok.prototype.DebugSnapshot.SkillState;
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

/** Exercita a tradução de regras em dados visuais sem relógio real, threads ou JFrame. */
class HudSkillFeedbackTest {
    private static final float DT = 1f / 30f;
    private static final float RADIUS = DebugWorld.PLAYER_RADIUS;
    private static final SkillCastCommand CRYO = new SkillCastCommand(CryostasisService.ID, 500, 340);
    private static final SkillCastCommand DASH = new SkillCastCommand(LightningStepService.ID, 500, 340);
    private static final DebugInput RIGHT = new DebugInput(false, false, false, true, false);
    private static final DebugInput LEFT = new DebugInput(false, false, true, false, false);
    private static final DebugInput DOWN = new DebugInput(false, true, false, false, false);

    /** Disponibilidade usa custo real e dá prioridade a morte, espera, movimento e cooldown. */
    @Test
    void mapeiaEstadosCustosEVigorSemConsultarOuModificarEntidades() {
        var none = SkillHudMapper.none();
        var cryo = SkillHudMapper.cryostasis(false, 100, 0, none);
        var dash = SkillHudMapper.lightningStep(false, 100, 0, false, false, none);
        assertEquals(CryostasisService.COST, cryo.cost());
        assertEquals(LightningStepService.COST, dash.cost());
        assertEquals(CryostasisService.ID, cryo.skillId());
        assertEquals(LightningStepService.ID, dash.skillId());
        assertEquals(SkillState.READY, cryo.state());
        assertEquals(SkillState.READY, dash.state());
        assertFalse(cryo.stateText().isBlank());
        assertFalse(dash.stateText().isBlank());
        assertEquals(SkillState.READY, SkillHudMapper.cryostasis(false, 55, 0, none).state());
        assertEquals(SkillState.READY, SkillHudMapper.lightningStep(false, 35, 0, false, false, none).state());
        assertEquals(SkillState.INSUFFICIENT_STAMINA, SkillHudMapper.cryostasis(false, 54.99f, 0, none).state());
        assertEquals(SkillState.INSUFFICIENT_STAMINA,
                SkillHudMapper.lightningStep(false, 34.99f, 0, false, false, none).state());
        assertEquals(SkillState.COOLDOWN, SkillHudMapper.cryostasis(false, 0, 3, none).state());
        assertEquals(SkillState.COOLDOWN, SkillHudMapper.lightningStep(false, 0, 4, false, false, none).state());
        assertEquals(SkillState.ACTIVE, SkillHudMapper.lightningStep(false, 0, 4, true, false, none).state());
        assertEquals(SkillState.AWAITING_LANDING,
                SkillHudMapper.lightningStep(false, 0, 3, true, true, none).state());
        assertEquals(SkillState.DEAD, SkillHudMapper.cryostasis(true, 100, 0, none).state());
        assertEquals(SkillState.DEAD, SkillHudMapper.lightningStep(true, 100, 0, true, true, none).state());
    }

    /** Cada resposta real do domínio tem um feedback inequívoco, inclusive direção zero. */
    @Test
    void traduzTodosOsResultadosDeCastSemInventarMotivos() {
        var cryoKinds = List.of(SkillFeedbackKind.ACCEPTED, SkillFeedbackKind.INVALID_TARGET,
                SkillFeedbackKind.DEAD, SkillFeedbackKind.COOLDOWN, SkillFeedbackKind.INSUFFICIENT_STAMINA);
        var cryoStatuses = List.of(CryostasisService.Status.CAST, CryostasisService.Status.INVALID,
                CryostasisService.Status.DEAD, CryostasisService.Status.COOLDOWN, CryostasisService.Status.STAMINA);
        for (int i = 0; i < cryoStatuses.size(); i++) {
            var feedback = SkillHudMapper.feedback(cryoStatuses.get(i), 1.2f);
            assertEquals(cryoKinds.get(i), feedback.kind());
            assertFalse(feedback.message().isBlank());
            assertEquals(1.2f, feedback.remaining());
        }
        var dashKinds = List.of(SkillFeedbackKind.ACCEPTED, SkillFeedbackKind.INVALID_TARGET,
                SkillFeedbackKind.DEAD, SkillFeedbackKind.ZERO_DIRECTION, SkillFeedbackKind.COOLDOWN,
                SkillFeedbackKind.INSUFFICIENT_STAMINA, SkillFeedbackKind.AWAITING_LANDING);
        var dashStatuses = List.of(LightningStepService.Status.STARTED, LightningStepService.Status.INVALID,
                LightningStepService.Status.DEAD, LightningStepService.Status.ZERO_DIRECTION,
                LightningStepService.Status.COOLDOWN, LightningStepService.Status.STAMINA,
                LightningStepService.Status.LANDING_PENDING);
        for (int i = 0; i < dashStatuses.size(); i++) {
            var feedback = SkillHudMapper.feedback(dashStatuses.get(i), 1.2f);
            assertEquals(dashKinds.get(i), feedback.kind());
            assertFalse(feedback.message().isBlank());
        }
    }

    /** A mensagem dura somente o tempo simulado, sem modificar o valor já publicado. */
    @Test
    void feedbackExpiraPorDeltaEEsperaPodeSerLiberadaSemRenovarHabilidade() {
        var accepted = SkillHudMapper.feedback(LightningStepService.Status.STARTED, 1.2f);
        var advanced = SkillHudMapper.elapse(accepted, 0.2f);
        assertEquals(1, advanced.remaining(), 0.00001);
        assertEquals(1.2f, accepted.remaining());
        assertEquals(SkillFeedbackKind.NONE, SkillHudMapper.elapse(advanced, 1).kind());
        assertTrue(SkillHudMapper.elapse(advanced, 1).message().isBlank());
        var pending = SkillHudMapper.lightningStep(false, 65, 2, false, true,
                SkillHudMapper.feedback(LightningStepService.Status.LANDING_PENDING, 1.2f));
        assertEquals(SkillState.AWAITING_LANDING, pending.state());
        var released = SkillHudMapper.lightningStep(false, 65, 2, false, false, SkillHudMapper.none());
        assertEquals(SkillState.COOLDOWN, released.state());
        assertEquals(2, released.cooldown());
        assertEquals(65, released.stamina());
        assertEquals(SkillState.READY,
                SkillHudMapper.lightningStep(false, 65, 0, false, false, SkillHudMapper.none()).state());
    }

    /** Duas skills aceitas na mesma fila mantêm o saldo e as recusas posteriores exatas. */
    @Test
    void snapshotRefleteAceiteFifoERecusaPorCooldownSemGastoExtra() {
        var world = world();
        cast(world, CRYO, DASH);
        var started = world.snapshot();
        assertEquals(10, started.stamina());
        assertEquals(SkillState.COOLDOWN, started.cryostasisHud().state());
        assertEquals(SkillState.ACTIVE, started.lightningStepHud().state());
        assertEquals(SkillFeedbackKind.ACCEPTED, started.cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.ACCEPTED, started.lightningStepHud().feedback().kind());
        assertEquals(started.cryostasisCooldown(), started.cryostasisHud().cooldown());
        assertEquals(started.lightningStepCooldown(), started.lightningStepHud().cooldown());
        advance(world, 8);
        cast(world, CRYO, DASH);
        var refused = world.snapshot();
        assertEquals(14.5f, refused.stamina());
        assertEquals(SkillFeedbackKind.COOLDOWN, refused.cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.COOLDOWN, refused.lightningStepHud().feedback().kind());
        assertEquals(SkillState.COOLDOWN, refused.lightningStepHud().state());
    }

    /** Falta de vigor aparece nas duas skills sem ativação, gasto, cooldown ou mensagem infinita. */
    @Test
    void vigorInsuficienteBloqueiaAmbasEFeedbackTerminaSemNovosComandos() {
        var world = depletedWorld();
        cast(world, CRYO, DASH);
        var refused = world.snapshot();
        assertEquals(0.5f, refused.stamina());
        assertEquals(SkillState.INSUFFICIENT_STAMINA, refused.cryostasisHud().state());
        assertEquals(SkillState.INSUFFICIENT_STAMINA, refused.lightningStepHud().state());
        assertEquals(SkillFeedbackKind.INSUFFICIENT_STAMINA, refused.cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.INSUFFICIENT_STAMINA, refused.lightningStepHud().feedback().kind());
        assertEquals(0, refused.cryostasisCooldown());
        assertEquals(0, refused.lightningStepCooldown());
        assertTrue(refused.iceZones().isEmpty());
        assertFalse(refused.lightningStepActive());
        advance(world, 37);
        assertEquals(SkillFeedbackKind.NONE, world.snapshot().cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.NONE, world.snapshot().lightningStepHud().feedback().kind());
        assertEquals(SkillState.INSUFFICIENT_STAMINA, world.snapshot().lightningStepHud().state());
    }

    /** Dados inválidos e cursor na origem produzem motivos distintos sem efeitos colaterais. */
    @Test
    void errosDeAlvoEDirecaoSaoFiéisAosResultadosDoDominio() {
        var world = world();
        var origin = world.snapshot().actors().get(0);
        cast(world, new SkillCastCommand(CryostasisService.ID, Float.NaN, 340),
                new SkillCastCommand(LightningStepService.ID, origin.x(), origin.y()));
        var snapshot = world.snapshot();
        assertEquals(SkillFeedbackKind.INVALID_TARGET, snapshot.cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.ZERO_DIRECTION, snapshot.lightningStepHud().feedback().kind());
        assertEquals(100, snapshot.stamina());
        assertEquals(0, snapshot.lightningStepCooldown());
        assertEquals(origin.x(), snapshot.actors().get(0).x());
        assertEquals(origin.y(), snapshot.actors().get(0).y());
        cast(world, new SkillCastCommand(LightningStepService.ID, Float.POSITIVE_INFINITY, 340));
        assertEquals(SkillFeedbackKind.INVALID_TARGET, world.snapshot().lightningStepHud().feedback().kind());
    }

    /** Um alvo além da parede não significa bloqueio quando o alcance termina antes dela. */
    @Test
    void naoInventaBloqueioQuandoAlvoFicaAlemDeUmaParedeNaoAlcancada() {
        var world = world();
        cast(world, new SkillCastCommand(LightningStepService.ID, 700, 340));
        advance(world, 8);
        assertEquals(285, world.snapshot().actors().get(0).x(), 0.0001);
        assertNotEquals(SkillFeedbackKind.SOLID_OR_BOUNDARY, world.snapshot().lightningStepHud().feedback().kind());
        assertEquals(SkillState.COOLDOWN, world.snapshot().lightningStepHud().state());
    }

    /** Parede e limite realmente alcançados publicam o bloqueio medido pela simulação. */
    @Test
    void paredeELimiteRealmenteBloqueantesPublicamFeedbackTemporario() {
        var wall = blockedWorld();
        var blocked = wall.snapshot();
        assertEquals(328, blocked.actors().get(0).x(), 0.0001);
        assertEquals(SkillFeedbackKind.SOLID_OR_BOUNDARY, blocked.lightningStepHud().feedback().kind());
        assertEquals(SkillState.COOLDOWN, blocked.lightningStepHud().state());
        advance(wall, 37);
        assertEquals(SkillFeedbackKind.NONE, wall.snapshot().lightningStepHud().feedback().kind());
        var edge = world();
        cast(edge, new SkillCastCommand(LightningStepService.ID, 0, 340));
        advance(edge, 8);
        assertEquals(RADIUS, edge.snapshot().actors().get(0).x(), 0.0001);
        assertEquals(SkillFeedbackKind.SOLID_OR_BOUNDARY, edge.snapshot().lightningStepHud().feedback().kind());
    }

    /** Recuar para sair de arbusto não é evidência de parede nem deve ser rotulado assim. */
    @Test
    void recuperacaoDentroDeArbustoNaoEhReportadaComoParedeOuLimite() {
        var world = world();
        for (int i = 0; i < 14; i++) world.tick(LEFT, DT);
        for (int i = 0; i < 14; i++) world.tick(DOWN, DT);
        var origin = world.snapshot().actors().get(0);
        assertEquals(35, origin.x(), 1);
        assertEquals(440, origin.y(), 1);
        cast(world, new SkillCastCommand(LightningStepService.ID, 700, origin.y()));
        advance(world, 8);
        var recovered = world.snapshot();
        assertEquals(142, recovered.actors().get(0).x(), 0.0001);
        assertFalse(recovered.lightningStepAwaitingLanding());
        assertNotEquals(SkillFeedbackKind.SOLID_OR_BOUNDARY, recovered.lightningStepHud().feedback().kind());
    }

    /** O dado publicado pelo domínio distingue varredura bloqueada de recuperação normal. */
    @Test
    void stepIndicaSomenteBloqueioFisicoAnteriorACorrecao() {
        var collisions = new CollisionService();
        var empty = new CollisionService.Space(960, 640, List.of(), List.of());
        var free = player(100, 100);
        var freeDash = service(collisions);
        assertEquals(LightningStepService.Status.STARTED,
                freeDash.cast(free, new SkillCastCommand(LightningStepService.ID, 500, 100), RADIUS, empty));
        assertFalse(freeDash.update(free, 0.1f, RADIUS, empty).blocked());

        var wallSpace = new CollisionService.Space(960, 640,
                List.of(new CollisionService.Obstacle(140, 0, 10, 640)), List.of());
        var walled = player(100, 100);
        var wallDash = service(collisions);
        wallDash.cast(walled, new SkillCastCommand(LightningStepService.ID, 500, 100), RADIUS, wallSpace);
        assertTrue(wallDash.update(walled, 0.1f, RADIUS, wallSpace).blocked());

        var bushSpace = new CollisionService.Space(960, 640,
                List.of(new CollisionService.Obstacle(180, 70, 80, 60, CollisionCategory.SOFT_OBSTACLE)), List.of());
        var recovering = player(100, 100);
        var selective = service(collisions);
        selective.cast(recovering, new SkillCastCommand(LightningStepService.ID, 500, 100), RADIUS, bushSpace);
        LightningStepService.Step finalStep = null;
        for (int i = 0; i < 8; i++) finalStep = selective.update(recovering, DT, RADIUS, bushSpace);
        assertNotNull(finalStep);
        assertEquals(162, recovering.getPosition().x(), 0.0001);
        assertFalse(finalStep.blocked(), "O recuo de saída não se confunde com impacto sólido");
    }

    /** Morte prevalece sobre recurso disponível e arena nova elimina feedback e estados antigos. */
    @Test
    void morteBloqueiaDisponibilidadeEReinicioRestauraHud() {
        var dead = world();
        for (int i = 0; i < 1800 && !dead.snapshot().gameOver(); i++) dead.tick(DebugInput.NONE, DT);
        assertTrue(dead.snapshot().gameOver());
        assertEquals(SkillState.DEAD, dead.snapshot().cryostasisHud().state());
        assertEquals(SkillState.DEAD, dead.snapshot().lightningStepHud().state());

        var old = world();
        cast(old, CRYO, DASH);
        advance(old, 2);
        assertFalse(old.snapshot().lightningTrails().isEmpty());
        var restarted = world().snapshot();
        assertEquals(100, restarted.stamina());
        assertEquals(SkillState.READY, restarted.cryostasisHud().state());
        assertEquals(SkillState.READY, restarted.lightningStepHud().state());
        assertEquals(SkillFeedbackKind.NONE, restarted.cryostasisHud().feedback().kind());
        assertEquals(SkillFeedbackKind.NONE, restarted.lightningStepHud().feedback().kind());
        assertFalse(restarted.lightningStepActive());
        assertFalse(restarted.lightningStepAwaitingLanding());
        assertTrue(restarted.lightningTrails().isEmpty());
        assertEquals(3, restarted.softObstacles().size());
    }

    /** Captura quatro estados do painel real; renderizar repetidamente não altera a simulação. */
    @Test
    void capturaHudProntoSemVigorAtivoEBloqueadoSemModificarMundo() throws Exception {
        var ready = world();
        var empty = depletedWorld();
        cast(empty, CRYO, DASH);
        var active = world();
        cast(active, CRYO, DASH);
        advance(active, 2);
        var blocked = blockedWorld();
        List<DebugWorld> worlds = List.of(ready, empty, active, blocked);
        List<DebugSnapshot> snapshots = worlds.stream().map(DebugWorld::snapshot).toList();
        List<String> titles = List.of("PRONTO: custo e disponibilidade", "SEM VIGOR: recusa fiel e temporária",
                "ATIVO: Q e Shift na mesma fila", "BLOQUEIO REAL: parede alcançada, recarga mantida");
        int header = 36, frameHeight = DebugPanel.PANEL_HEIGHT + header;
        var image = new BufferedImage(DebugPanel.PANEL_WIDTH * 2, frameHeight * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(10, 21, 35));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
            for (int i = 0; i < worlds.size(); i++) {
                int x = i % 2 * DebugPanel.PANEL_WIDTH, y = i / 2 * frameHeight;
                graphics.setColor(new Color(207, 234, 184));
                graphics.drawString(titles.get(i), x + 18, y + 24);
                Graphics2D frame = (Graphics2D) graphics.create();
                try {
                    frame.translate(x, y + header);
                    frame.clipRect(0, 0, DebugPanel.PANEL_WIDTH, DebugPanel.PANEL_HEIGHT);
                    var panel = new DebugPanel(worlds.get(i)::snapshot);
                    panel.setShowHitboxes(true);
                    panel.render(frame, snapshots.get(i));
                } finally {
                    frame.dispose();
                }
                assertEquals(snapshots.get(i), worlds.get(i).snapshot());
            }
        } finally {
            graphics.dispose();
        }
        Path output = Path.of("target/task009-hud.png");
        Files.createDirectories(output.getParent());
        assertTrue(ImageIO.write(image, "png", output.toFile()));
    }

    /** Constrói a arena jogável com sorteios reproduzíveis, sem iniciar o runtime Swing. */
    private static DebugWorld world() { return DebugWorld.demo(new Random(9)); }

    /** Consome vigor somente pela ação provisória existente, preservando as regras do domínio. */
    private static DebugWorld depletedWorld() {
        var world = world();
        world.tickQueued(DebugInput.NONE, DT, 5, List.of());
        assertEquals(0, world.snapshot().stamina());
        return world;
    }

    /** Posiciona por WASD e executa um dash real contra a primeira parede histórica. */
    private static DebugWorld blockedWorld() {
        var world = world();
        for (int i = 0; i < 8; i++) world.tick(RIGHT, DT);
        cast(world, new SkillCastCommand(LightningStepService.ID, 700, 340));
        advance(world, 8);
        return world;
    }

    /** Transporta os comandos em FIFO pelo mesmo ponto de integração usado pelo runtime. */
    private static void cast(DebugWorld world, SkillCastCommand... commands) {
        world.tickQueued(DebugInput.NONE, DT, 0, List.of(commands));
    }

    /** Avança somente o tempo de simulação; não depende de sleeps nem de relógio do SO. */
    private static void advance(DebugWorld world, int ticks) {
        for (int i = 0; i < ticks; i++) world.tick(DebugInput.NONE, DT);
    }

    /** Reutiliza a física autoritativa e a política seletiva para inspecionar o relatório do passo. */
    private static LightningStepService service(CollisionService collisions) {
        return new LightningStepService(new MovementService(collisions), new StaminaService(), collisions,
                CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);
    }

    /** Instancia o jogador real sem sockets, banco ou referências da interface. */
    private static Player player(float x, float y) {
        var archetype = new PlayerArchetype(RaceRegistry.withDefaults().get("human"), 30, 8, 14, null, null);
        return new PlayerFactory(new VitalityService()).create(1, "HUD", archetype,
                new Position(x, y), new PlayerConnection() {
                    @Override public void send(Object packet) { /* simulação local */ }
                    @Override public void disconnect() { /* nenhuma conexão */ }
                });
    }
}
