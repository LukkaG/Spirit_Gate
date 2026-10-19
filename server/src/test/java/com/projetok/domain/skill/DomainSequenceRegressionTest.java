package com.projetok.domain.skill;

import com.projetok.domain.ai.PassiveBehavior;
import com.projetok.domain.combat.CombatService;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.factory.MobArchetype;
import com.projetok.domain.factory.MobFactory;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.movement.CollisionCategory;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.MovementService;
import com.projetok.domain.movement.SpatialBodyIndex;
import com.projetok.domain.probability.ProbabilityService;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.resource.StaminaService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static com.projetok.domain.skill.LightningStepTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regride sequências de serviços reais sem relógio do SO, interface ou novos baselines. */
class DomainSequenceRegressionTest {
    private static final float DT = 1f / 30f;
    private static final float MOB_RADIUS = 19f;
    private static final float WALK_SPEED = 215f;

    // --- Dash, dano recebido e retorno à colisão normal ---

    /** Travessia não causa dano nem invulnerabilidade; depois, caminhada e ataque básico continuam normais. */
    @Test
    void atravessaAtorRecebeDanoERetomaMovimentoEAtaqueSemCusto() {
        var player = player(100, 100);
        var mob = mob(175, 100);
        var collisions = new CollisionService();
        var movement = new MovementService(collisions);
        var combat = new CombatService(new GameEventBus());
        var bodies = List.of(new CollisionService.Body(player, RADIUS), new CollisionService.Body(mob, MOB_RADIUS));
        var index = new SpatialBodyIndex(bodies, 16);
        var space = new CollisionService.Space(960, 640, List.of(), bodies, index);
        var dash = selectiveService();

        assertEquals(LightningStepService.Status.STARTED, dash.cast(player, right(), RADIUS, space));
        dash.update(player, DT, RADIUS, space);
        assertEquals(120, player.getPosition().x(), 0.0001);
        assertEquals(120, mob.require(HealthComponent.class).current(), "Mover não aplica dano aos corpos atravessados");
        assertTrue(Math.abs(mob.getPosition().x() - player.getPosition().x()) <= 58,
                "O atacante está no alcance usado pela arena durante o dash");
        assertEquals(6, combat.attack(mob, player));
        assertEquals(94, player.require(HealthComponent.class).current(), "O dash não concede invulnerabilidade");
        for (int i = 1; i < 8; i++) dash.update(player, DT, RADIUS, space);

        assertFalse(dash.active());
        assertFalse(dash.awaitingLanding());
        assertEquals(new Position(250, 100), player.getPosition());
        assertEquals(120, mob.require(HealthComponent.class).current());
        assertTrue(mob.require(StatusEffectsComponent.class).snapshot().isEmpty());
        for (int i = 0; i < 5; i++) {
            movement.moveDirected(player, new Position(-1, 0), WALK_SPEED, DT, RADIUS, space);
        }
        var touching = player.getPosition();
        assertTrue(touching.x() - mob.getPosition().x() >= RADIUS + MOB_RADIUS);
        movement.moveDirected(player, new Position(-1, 0), WALK_SPEED, DT, RADIUS, space);
        assertEquals(touching, player.getPosition(), "A caminhada volta a ser bloqueada pelo mesmo ator");
        assertTrue(collisions.isFree(player, touching.x(), touching.y(), RADIUS, space));

        assertEquals(15, combat.attack(player, mob));
        assertEquals(105, mob.require(HealthComponent.class).current());
        assertEquals(65, player.require(StaminaComponent.class).current(), "Andar e atacar não consomem vigor");
        assertEquals(94, player.require(HealthComponent.class).current());
    }

    // --- Categoria de cenário não altera dano, sorteios ou temporizadores ---

    /** Trocar somente a categoria conserva os cinco pulsos, Freeze, ataques e estados quadro a quadro. */
    @Test
    void categoriaDeObstaculoNaoAlteraSequenciaDeCombateECongelamento() {
        var solid = combatTrace(CollisionCategory.SOLID_OBSTACLE);
        var soft = combatTrace(CollisionCategory.SOFT_OBSTACLE);
        assertEquals(solid, soft, "Geometria idêntica não muda dano, duração ou probabilidades do combate");
        assertEquals(5, solid.draws());
        assertEquals(5, solid.hits());
        assertEquals(70, solid.frames().get(solid.frames().size() - 1).mobHp(),
                "Cinco pulsos de sete e um ataque desarmado de quinze");
        assertEquals(88, solid.frames().get(solid.frames().size() - 1).playerHp(),
                "Dois ataques de seis foram permitidos enquanto o mob estava congelado");
        assertTrue(solid.frames().stream().filter(frame -> frame.freeze() > 0)
                .allMatch(frame -> frame.mobPosition().equals(new Position(250, 100))));
        assertTrue(solid.frames().get(solid.frames().size() - 1).mobPosition().y() < 100,
                "Após expirar o Freeze, a perseguição volta a deslocar o mob");
    }

    // --- Recuperação ocupada, grade espacial e movimento no tick seguinte ---

    /** Um corpo móvel com coordenada fracionária exige recuo e a grade continua correta após caminhar. */
    @Test
    void destinoOcupadoPorCorpoMovelCorrigeIndiceERetomaMovimento() {
        var player = player(100, 100);
        var mob = mob(400, 100);
        var collisions = new CollisionService();
        var movement = new MovementService(collisions);
        var stamina = new StaminaService();
        var playerBody = new CollisionService.Body(player, RADIUS);
        var mobBody = new CollisionService.Body(mob, MOB_RADIUS);
        var bodies = List.of(playerBody, mobBody);
        var index = new SpatialBodyIndex(bodies, 16);
        var indexed = new CollisionService.Space(960, 640, List.of(), bodies, index);
        var reference = new CollisionService.Space(960, 640, List.of(), bodies);
        var dash = selectiveService();
        assertEquals(LightningStepService.Status.STARTED, dash.cast(player, right(), RADIUS, indexed));
        dash.update(player, 0.1f, RADIUS, indexed);

        float movingTargetX = Math.nextAfter(250f, Double.POSITIVE_INFINITY);
        mob.setPosition(new Position(movingTargetX, 100));
        index.update(mob);
        dash.update(player, 0.15f, RADIUS, indexed);
        var recovered = player.getPosition();
        assertEquals(movingTargetX - RADIUS - MOB_RADIUS, recovered.x(), Math.ulp(movingTargetX) * 2);
        assertTrue(collisions.isFree(player, recovered.x(), recovered.y(), RADIUS, indexed));
        assertFalse(collisions.isFree(player, Math.nextAfter(recovered.x(), Double.POSITIVE_INFINITY),
                recovered.y(), RADIUS, indexed), "O próximo float já penetraria o corpo atual");
        assertTrue(index.query(recovered.x(), recovered.y(), recovered.x(), recovered.y()).contains(playerBody));
        assertFalse(index.query(250, 100, 250, 100).contains(playerBody), "A célula anterior não retém o jogador");

        for (int i = 0; i < 20; i++) {
            dash.update(player, DT, RADIUS, indexed);
            stamina.regenerate(player, DT);
            movement.moveDirected(player, i < 12 ? new Position(-1, 0) : new Position(0, 1),
                    WALK_SPEED, DT, RADIUS, indexed);
        }
        var walking = player.getPosition();
        assertEquals(recovered.x() - WALK_SPEED * DT * 12, walking.x(), 0.0002);
        assertEquals(100 + WALK_SPEED * DT * 8, walking.y(), 0.0002);
        assertTrue(index.query(walking.x(), walking.y(), walking.x(), walking.y()).contains(playerBody));
        assertFalse(index.query(recovered.x(), recovered.y(), recovered.x(), recovered.y()).contains(playerBody));
        assertTrue(index.query(movingTargetX, 100, movingTargetX, 100).contains(mobBody));
        for (var point : List.of(walking, recovered, new Position(movingTargetX, 100),
                new Position(movingTargetX + RADIUS + MOB_RADIUS, 100), new Position(100, 100))) {
            assertEquals(collisions.isFree(player, point.x(), point.y(), RADIUS, reference),
                    collisions.isFree(player, point.x(), point.y(), RADIUS, indexed),
                    "O índice mantém o mesmo resultado da consulta sem grade após recuo e WASD");
        }
        assertEquals(75, player.require(StaminaComponent.class).current());
        assertEquals(120, mob.require(HealthComponent.class).current());
        assertFalse(dash.active());
        assertFalse(dash.awaitingLanding());
    }

    // --- Vizinhos float de tangência e borda ---

    /** Um ULP distingue origem válida de sobreposição; o dash conserva raio, custo e duração. */
    @Test
    void floatsAdjacentesATangenciaELimiteContinuamSeguros() {
        var collisions = new CollisionService();
        var wall = new CollisionService.Space(960, 640,
                List.of(new CollisionService.Obstacle(200, 50, 15, 100)), List.of());
        float tangent = 182;
        float outside = Math.nextAfter(tangent, Double.NEGATIVE_INFINITY);
        float inside = Math.nextAfter(tangent, Double.POSITIVE_INFINITY);
        var player = player(outside, 100);
        assertTrue(collisions.isFree(player, outside, 100, RADIUS, wall));
        assertTrue(collisions.isFree(player, tangent, 100, RADIUS, wall));
        assertFalse(collisions.isFree(player, inside, 100, RADIUS, wall));
        var dash = selectiveService();
        assertEquals(LightningStepService.Status.STARTED, dash.cast(player, right(), RADIUS, wall));
        var stopped = dash.update(player, 0.25f, RADIUS, wall);
        assertEquals(tangent, player.getPosition().x());
        assertTrue(stopped.blocked());
        assertFalse(dash.active());
        assertEquals(65, player.require(StaminaComponent.class).current());

        var invalid = player(inside, 100);
        var refused = selectiveService();
        assertEquals(LightningStepService.Status.INVALID, refused.cast(invalid, right(), RADIUS, wall));
        assertEquals(new Position(inside, 100), invalid.getPosition());
        assertEquals(100, invalid.require(StaminaComponent.class).current());
        assertEquals(0, refused.cooldown());

        var empty = emptySpace();
        float nearBorder = Math.nextAfter(RADIUS, Double.POSITIVE_INFINITY);
        var edgePlayer = player(nearBorder, 100);
        assertFalse(collisions.isFree(edgePlayer, Math.nextAfter(RADIUS, Double.NEGATIVE_INFINITY), 100, RADIUS, empty));
        assertTrue(collisions.isFree(edgePlayer, RADIUS, 100, RADIUS, empty));
        var edgeDash = selectiveService();
        assertEquals(LightningStepService.Status.STARTED, edgeDash.cast(edgePlayer,
                new SkillCastCommand(LightningStepService.ID, 0, 100), RADIUS, empty));
        edgeDash.update(edgePlayer, 0.25f, RADIUS, empty);
        assertEquals(new Position(RADIUS, 100), edgePlayer.getPosition());
        assertFalse(edgeDash.active());
        assertFalse(edgeDash.awaitingLanding());
        assertEquals(65, edgePlayer.require(StaminaComponent.class).current());
        assertEquals(3.75f, edgeDash.cooldown());
    }

    // --- Fixtures e roteiros determinísticos ---

    /** Posiciona o alvo sem alterar a direção positiva fixada no lançamento. */
    private static SkillCastCommand right() { return new SkillCastCommand(LightningStepService.ID, 800, 100); }

    /** Cria o mesmo arquétipo real da arena, incluindo combate, raça, vida e efeitos. */
    private static Mob mob(float x, float y) {
        var archetype = new MobArchetype(RaceRegistry.withDefaults().get("demon"), "Alvo de regressão",
                1, 12, 4, 9, null, LootTable.of(), new PassiveBehavior());
        return new MobFactory(new VitalityService()).spawn(200, archetype, new Position(x, y));
    }

    /** Captura somente valores, permitindo comparar as duas categorias sem hashes de apresentação. */
    private record Frame(int mobHp, int playerHp, float freeze, Position mobPosition) { }

    /** Resultado imutável do roteiro de combate, incluindo o número exato de sorteios e pulsos. */
    private record Trace(List<Frame> frames, int draws, int hits) {
        /** Preserva a sequência para comparações posteriores sem compartilhar a lista mutável. */
        private Trace { frames = List.copyOf(frames); }
    }

    /** Repete cinco pulsos e tentativas de movimento; a única variável é a categoria do mesmo retângulo. */
    private static Trace combatTrace(CollisionCategory category) {
        var player = player(200, 100);
        var mob = mob(250, 100);
        var effects = new StatusEffectService();
        var combat = new CombatService(new GameEventBus());
        var sequence = new ControlledSequence(0.29, 0.30, 0.29, 0.30, 0.99);
        var cryo = new CryostasisService(combat, new StaminaService(), effects, new ProbabilityService(sequence));
        var movement = new MovementService(new CollisionService());
        var bodies = List.of(new CollisionService.Body(player, RADIUS), new CollisionService.Body(mob, MOB_RADIUS));
        var space = new CollisionService.Space(960, 640,
                List.of(new CollisionService.Obstacle(220, 70, 10, 60, category)), bodies,
                new SpatialBodyIndex(bodies, 16));
        var targets = List.of(new CryostasisService.Target(mob, MOB_RADIUS));
        var result = cryo.cast(player, new SkillCastCommand(CryostasisService.ID, 250, 100), 960, 640, targets);
        assertEquals(CryostasisService.Status.CAST, result.status());
        assertEquals(7, result.hits().get(0).damage());
        assertEquals(1, remaining(mob));
        assertEquals(6, combat.attack(mob, player));
        var frames = new ArrayList<Frame>();
        frames.add(frame(mob, player.require(HealthComponent.class).current()));
        int hits = result.hits().size();
        for (int tick = 1; tick <= 75; tick++) {
            effects.update(mob, DT);
            var pulse = cryo.update(DT, targets);
            for (var hit : pulse) assertEquals(7, hit.damage());
            hits += pulse.size();
            movement.moveDirected(mob, new Position(0, -1), 92, DT, MOB_RADIUS, space);
            if (tick == 20) {
                assertTrue(remaining(mob) > 0);
                assertEquals(6, combat.attack(mob, player));
            }
            frames.add(frame(mob, player.require(HealthComponent.class).current()));
        }
        assertTrue(cryo.zones().isEmpty());
        assertEquals(0, remaining(mob));
        assertEquals(15, combat.attack(player, mob));
        frames.add(frame(mob, player.require(HealthComponent.class).current()));
        return new Trace(frames, sequence.index, hits);
    }

    /** Lê somente os valores usados nas invariantes do roteiro. */
    private static Frame frame(Mob mob, int playerHp) {
        return new Frame(mob.require(HealthComponent.class).current(), playerHp, remaining(mob), mob.getPosition());
    }

    /** A fábrica anexa o armazenamento de efeitos antes de iniciar a sequência. */
    private static float remaining(Mob mob) { return mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE); }

    /** Falha imediatamente se algum caminho consumir aleatoriedade fora dos cinco pulsos planejados. */
    private static final class ControlledSequence implements RandomGenerator {
        private final double[] samples;
        private int index;

        /** Copia o roteiro para impedir alterações externas durante o teste. */
        private ControlledSequence(double... samples) { this.samples = samples.clone(); }

        /** O contrato reutilizável de probabilidade usa amostras uniformes de double. */
        @Override public long nextLong() { throw new AssertionError("Distribuição inesperada"); }

        /** Entrega uma tentativa independente por pulso e denuncia tentativas extras. */
        @Override public double nextDouble() {
            if (index >= samples.length) throw new AssertionError("Sorteio extra fora do roteiro");
            return samples[index++];
        }
    }
}
