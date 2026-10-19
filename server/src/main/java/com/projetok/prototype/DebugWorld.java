package com.projetok.prototype;

import com.projetok.domain.ai.PassiveBehavior;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.CollisionCategory;
import com.projetok.domain.movement.CollisionPolicy;
import com.projetok.domain.movement.MovementService;
import com.projetok.domain.movement.SpatialBodyIndex;
import com.projetok.domain.combat.CombatService;
import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.resource.StaminaService;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.CryostasisZone;
import com.projetok.domain.skill.LightningStepService;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.probability.ProbabilityService;
import java.util.random.RandomGenerator;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityDeathEvent;
import com.projetok.domain.factory.MobArchetype;
import com.projetok.domain.factory.MobFactory;
import com.projetok.domain.factory.PlayerArchetype;
import com.projetok.domain.factory.PlayerFactory;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.RaceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Mundo mínimo AUTORITATIVO para prototipagem. Só a thread de simulação pode alterar
 * suas entidades; a interface recebe cópias imutáveis por meio de {@link #snapshot()}.
 * Não depende de Swing, de PostgreSQL nem de Kryonet. Um futuro adaptador de rede poderá
 * substituir DebugInput por comandos remotos mantendo as mesmas regras de colisão.
 */
public final class DebugWorld {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 640;
    private final int worldWidth, worldHeight;

    /** Dimensões físicas do mapa atual; as constantes conservam o cenário histórico. */
    public int worldWidth() { return worldWidth; }
    public int worldHeight() { return worldHeight; }
    public static final float PLAYER_RADIUS = 18f;
    public static final float MOB_RADIUS = 19f;
    public static final float WALK_SPEED = 215f;
    public static final float MELEE_REACH = 74f;
    public static final float ATTACK_DURATION = 0.18f;
    private DebugSnapshot.AttackView attack;
    private static final float MOB_SPEED = 92f;
    private static final float MOB_AGGRO = 320f;
    private static final float MOB_MELEE = 58f;
    private static final float PLAYER_COOLDOWN = 0.38f;
    private static final float MOB_COOLDOWN = 1.15f;
    private static final float HALF_CONE_COS = 0.7071068f; // cone de 90 graus

    private final GameEventBus events = new GameEventBus();
    private final CombatService combat = new CombatService(events);
    private final CollisionService collisions = new CollisionService();
    private final MovementService movement;
    private final LightningStepService lightningStep;
    private final StaminaService staminaService = new StaminaService();
    private final StatusEffectService statusEffects = new StatusEffectService();
    private final CryostasisService cryostasis;
    private boolean effortHeld;
    private final Player player;
    private final List<Mob> mobs = new ArrayList<>();
    private final CollisionService.Space collisionSpace;
    private static final List<CollisionService.Obstacle> ORIGINAL_OBSTACLES = List.of(
            new CollisionService.Obstacle(346, 112, 110, 228),
            new CollisionService.Obstacle(594, 310, 112, 174),
            new CollisionService.Obstacle(275, 500, 145, 44));
    private final List<CollisionService.Obstacle> obstacles;
    private final List<DemoObstacle> softObstacles;

    /** Associa arte e corpo sem inferir a hitbox pelo tamanho do desenho. */
    private record DemoObstacle(CollisionService.Obstacle body, DebugSnapshot.SoftObstacleKind kind) { }

    // Área demonstrativa ao sul do ponto inicial; não altera o cenário histórico.
    private static final List<DemoObstacle> DEMO_OBSTACLES = List.of(
            new DemoObstacle(new CollisionService.Obstacle(70, 420, 36, 40,
                    CollisionCategory.SOFT_OBSTACLE), DebugSnapshot.SoftObstacleKind.BUSH),
            new DemoObstacle(new CollisionService.Obstacle(160, 420, 36, 40,
                    CollisionCategory.SOFT_OBSTACLE), DebugSnapshot.SoftObstacleKind.BUSH),
            new DemoObstacle(new CollisionService.Obstacle(250, 425, 14, 30,
                    CollisionCategory.SOFT_OBSTACLE), DebugSnapshot.SoftObstacleKind.POST));
    private final List<DebugSnapshot.EffectView> effects = new ArrayList<>();
    private final List<DebugSnapshot.LightningTrailView> lightningTrails = new ArrayList<>();
    private static final float TRAIL_DURATION = 0.18f;
    private static final float SKILL_FEEDBACK_DURATION = 1.2f;
    private DebugSnapshot.SkillFeedbackView cryostasisFeedback = SkillHudMapper.none();
    private DebugSnapshot.SkillFeedbackView lightningStepFeedback = SkillHudMapper.none();
    private final float[] enemyCooldowns = new float[3];
    private float facingX = 1f;
    private float facingY = 0f;
    private float attackCooldown;
    private long ticks;
    private final List<DirectionalAnimation.Signal> animationSignals = new ArrayList<>();

    /** Copia eventos reais do ultimo tick sem modificar o contrato historico do snapshot. */
    public List<DirectionalAnimation.Signal> animationSignals() { return List.copyOf(animationSignals); }
    private int defeated;
    private String lastEvent = "Explore a arena e derrote os tres espiritos.";

    /** Instancia jogador e mobs usando as fábricas reais do domínio existente. */
    public DebugWorld() {
        this(RandomGenerator.getDefault());
    }

    /** Permite reproduzir os sorteios de gameplay sem acoplar testes à interface. */
    public DebugWorld(RandomGenerator random) {
        this(random, MovementService.Response.SOFT_SLIDE);
    }

    /** Permite comparar a resposta nova com a referência histórica sem alterar os baselines. */
    public DebugWorld(RandomGenerator random, MovementService.Response response) {
        this(random, response, false);
    }

    /** Cria a arena jogável com dois arbustos e um poste de colisão leve. */
    public static DebugWorld demo() {
        return demo(RandomGenerator.getDefault());
    }

    /** Reproduz o cenário demonstrativo com sorteios controláveis nos testes. */
    public static DebugWorld demo(RandomGenerator random) {
        return new DebugWorld(random, MovementService.Response.SOFT_SLIDE, true);
    }

    /** Mantém os construtores históricos e compartilha toda a simulação entre os cenários. */
    private DebugWorld(RandomGenerator random, MovementService.Response response, boolean demo) {
        this(random, response, demo, null);
    }

    /** Variante isolada por dados locais; demo() e construtores historicos permanecem iguais. */
    public static DebugWorld forMap(LocalArenaMap map, RandomGenerator random) {
        return new DebugWorld(random, MovementService.Response.SOFT_SLIDE, false, java.util.Objects.requireNonNull(map));
    }

    private DebugWorld(RandomGenerator random, MovementService.Response response, boolean demo, LocalArenaMap map) {
        worldWidth = map == null ? WIDTH : map.width();
        worldHeight = map == null ? HEIGHT : map.height();
        softObstacles = map == null ? (demo ? DEMO_OBSTACLES : List.of()) : map.physical().stream()
                .filter(p -> p.category() == CollisionCategory.SOFT_OBSTACLE)
                .map(p -> new DemoObstacle(new CollisionService.Obstacle(p.x(),p.y(),p.width(),p.height(),p.category()),p.kind())).toList();
        List<CollisionService.Obstacle> geometry = new ArrayList<>(map == null ? ORIGINAL_OBSTACLES : map.obstacles().stream()
                .filter(p -> p.category() == CollisionCategory.SOLID_OBSTACLE).toList());
        softObstacles.forEach(obstacle -> geometry.add(obstacle.body()));
        obstacles = List.copyOf(geometry);
        movement = new MovementService(collisions, response);
        lightningStep = new LightningStepService(movement, staminaService, collisions,
                CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);
        cryostasis = new CryostasisService(combat, staminaService, statusEffects, new ProbabilityService(random));
        RaceRegistry races = RaceRegistry.withDefaults();
        VitalityService vitality = new VitalityService();
        player = new PlayerFactory(vitality).create(1, "Guardiao", new PlayerArchetype(
                        races.get("human"), 30, 8, 14, null, null),
                new Position(map == null ? 135 : map.spawnX(), map == null ? 340 : map.spawnY()), new PlayerConnection() {
                    @Override public void send(Object packet) { /* canal local não envia pacotes */ }
                    @Override public void disconnect() { /* nenhum socket aberto */ }
                });
        MobFactory factory = new MobFactory(vitality);
        float[][] locations = {{215, 148}, {812, 190}, {782, 550}};
        for (int i = 0; i < locations.length; i++) {
            MobArchetype archetype = new MobArchetype(races.get("demon"),
                    "Espírito Errante " + (i + 1), 1, 12, 4, 9, null,
                    LootTable.of(), new PassiveBehavior());
            mobs.add(factory.spawn(100 + i, archetype,
                    new Position(locations[i][0], locations[i][1])));
        }
        List<CollisionService.Body> bodies = new ArrayList<>();
        bodies.add(new CollisionService.Body(player, PLAYER_RADIUS));
        for (Mob mob : mobs) bodies.add(new CollisionService.Body(mob, MOB_RADIUS));
        collisionSpace = new CollisionService.Space(worldWidth, worldHeight, obstacles, bodies,
                new SpatialBodyIndex(bodies, 96f));
        events.subscribe(EntityDeathEvent.class, event -> {
            if (event.entity() == player) {
                lastEvent = "Voce caiu! Pressione R para reiniciar.";
            } else {
                defeated++;
                lastEvent = event.entity().getName() + " derrotado!";
            }
        });
    }

    /**
     * Executa um frame determinístico, com delta em segundos; só deve ser chamado
     * pelo proprietário do estado do mundo. Input nunca inclui posição absoluta.
     */
    public void tick(DebugInput input, float dt) {
        tickQueued(input, dt, -1);
    }

    /** Processa pressões conservadas pela fila; -1 mantém detecção por estado para chamadas antigas. */
    void tickQueued(DebugInput input, float dt, int effortPresses) {
        tickQueued(input, dt, effortPresses, List.of());
    }

    /** Coordena comandos de skill sem alterar a API anterior de entrada. */
    void tickQueued(DebugInput input, float dt, int effortPresses, List<SkillCastCommand> casts) {
        if (!Float.isFinite(dt) || dt <= 0f || dt > 0.1f) throw new IllegalArgumentException("dt fora do intervalo (0, 0.1]");
        ticks++;
        animationSignals.clear();
        // Retornos do HUD têm relógio próprio e não substituem o log histórico da arena.
        cryostasisFeedback = SkillHudMapper.elapse(cryostasisFeedback, dt);
        lightningStepFeedback = SkillHudMapper.elapse(lightningStepFeedback, dt);
        // Efeitos antigos avançam antes dos pulsos: um novo sucesso conserva 1 s inteiro.
        for (Mob mob : mobs) statusEffects.update(mob, dt);
        if (attack != null) {
            float remaining = attack.remaining() - dt;
            attack = remaining <= 0f ? null : new DebugSnapshot.AttackView(
                    attack.x(), attack.y(), attack.facingX(), attack.facingY(), remaining);
        }
        attackCooldown = Math.max(0f, attackCooldown - dt);
        for (int i = 0; i < enemyCooldowns.length; i++) {
            enemyCooldowns[i] = Math.max(0f, enemyCooldowns[i] - dt);
        }
        for (int i = effects.size() - 1; i >= 0; i--) {
            var effect = effects.get(i);
            float remaining = effect.remaining() - dt;
            if (remaining <= 0) effects.remove(i);
            else effects.set(i, new DebugSnapshot.EffectView(effect.x(), effect.y(),
                    effect.damage(), remaining, effect.received()));
        }
        for (int i = lightningTrails.size() - 1; i >= 0; i--) {
            var trail = lightningTrails.get(i);
            float remaining = trail.remaining() - dt;
            if (remaining <= 0) lightningTrails.remove(i);
            else lightningTrails.set(i, new DebugSnapshot.LightningTrailView(
                    trail.x0(), trail.y0(), trail.x1(), trail.y1(), remaining, trail.duration()));
        }
        publishHits(cryostasis.update(dt, cryostasisTargets()));
        boolean dashingAtStart = lightningStep.active() || lightningStep.awaitingLanding();
        var dashStep = lightningStep.update(player, dt, PLAYER_RADIUS, collisionSpace);
        if (dashStep.moved()) addLightningTrail(dashStep);
        // A política seletiva ignora atores e leves: redução real só indica sólido ou borda.
        if (dashStep.blocked()) lightningStepFeedback = SkillHudMapper.blockedFeedback(SKILL_FEEDBACK_DURATION);
        if (player.isDead()) return;
        staminaService.regenerate(player, dt);
        int activations = effortPresses >= 0 ? effortPresses : (input.effort() && !effortHeld ? 1 : 0);
        effortHeld = input.effort();
        // O tick fracionário final ainda pertence ao dash; WASD retorna no próximo tick.
        if (!dashingAtStart) movePlayer(input, dt);
        if (input.attack()) attackNearest();
        for (SkillCastCommand command : casts) {
            // Um único laço preserva a ordem de Q/Shift e o saldo compartilhado de vigor.
            if (command != null && LightningStepService.ID.equals(command.skillId())) {
                var status = lightningStep.cast(player, command, PLAYER_RADIUS, collisionSpace);
                lightningStepFeedback = SkillHudMapper.feedback(status, SKILL_FEEDBACK_DURATION);
                lastEvent = switch (status) {
                    case STARTED -> "Passo Relampago iniciado: 35 de vigor.";
                    case INVALID -> "Passo Relampago: alvo invalido.";
                    case DEAD -> "Passo Relampago: jogador derrotado.";
                    case ZERO_DIRECTION -> "Passo Relampago: escolha uma direcao.";
                    case COOLDOWN -> "Passo Relampago em recarga.";
                    case STAMINA -> "Vigor insuficiente para Passo Relampago (35).";
                    case LANDING_PENDING -> "Passo Relampago: aguardando uma saida livre.";
                };
                continue;
            }
            var result = cryostasis.cast(player, command, worldWidth, worldHeight, cryostasisTargets());
            cryostasisFeedback = SkillHudMapper.feedback(result.status(), SKILL_FEEDBACK_DURATION);
            lastEvent = switch (result.status()) {
                case CAST -> "Criostase lancada: zona de gelo ativa.";
                case INVALID -> "Criostase: alvo ou habilidade invalida.";
                case DEAD -> "Criostase: jogador derrotado.";
                case COOLDOWN -> "Criostase em recarga.";
                case STAMINA -> "Vigor insuficiente para Criostase (55).";
            };
            publishHits(result.hits());
        }
        updateEnemies(dt);
        if (!player.isDead()) for (int i = 0; i < activations; i++) performEffort();
    }

    /** Registra só trechos realmente percorridos, incluindo a curva X/Y da física. */
    private void addLightningTrail(LightningStepService.Step step) {
        for (int i = 1; i < step.path().size(); i++) {
            var from = step.path().get(i - 1); var to = step.path().get(i);
            if (!from.equals(to)) lightningTrails.add(new DebugSnapshot.LightningTrailView(
                    from.x(), from.y(), to.x(), to.y(), TRAIL_DURATION, TRAIL_DURATION));
        }
    }

    /** Fornece hitboxes dos mobs; a skill consulta vida e posição em cada pulso. */
    private List<CryostasisService.Target> cryostasisTargets() {
        return mobs.stream().map(mob -> new CryostasisService.Target(mob, MOB_RADIUS)).toList();
    }

    /** Publica os danos já aplicados por CombatService e preserva feedback de morte. */
    private void publishHits(List<CryostasisService.Hit> hits) {
        for (var hit : hits) {
            addEffect(hit.mob(), hit.damage(), false);
            if (hit.mob().isDead()) lastEvent = hit.mob().getName() + " derrotado!";
        }
    }

    // --- Vigor físico: ação temporária, sem dano ---

    /** Sincroniza a tecla ao reiniciar para exigir uma nova pressão após soltá-la. */
    void synchronizeEffort(boolean held) {
        effortHeld = held;
    }

    /** Verifica o custo antes de executar a ação provisória de esforço. */
    private void performEffort() {
        if (staminaService.tryConsume(player, 20f)) {
            lastEvent = "Esforco fisico realizado: 20 de vigor.";
        } else {
            lastEvent = "Vigor insuficiente para esforco fisico (20).";
        }
    }

    // --- Movimento e colisões ---

    /** Normaliza diagonais, move por eixos e respeita paredes, objetos e atores. */
    private void movePlayer(DebugInput input, float dt) {
        float x = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
        float y = (input.down() ? 1 : 0) - (input.up() ? 1 : 0);
        if (x == 0f && y == 0f) return;
        Position direction = movement.direction(x, y);
        x = direction.x();
        y = direction.y();
        facingX = x;
        facingY = y;
        movement.moveDirected(player, direction, WALK_SPEED, dt, PLAYER_RADIUS, collisionSpace());
    }

    /** Consulta posições atuais entre movimentos sequenciais dos atores. */
    private CollisionService.Space collisionSpace() {
        return collisionSpace;
    }

    // --- Elegibilidade de combate e dano delegado ao domínio ---

    /** Aplica cooldown, distância e cone ANTES de chamar CombatService.attack. */
    private void attackNearest() {
        if (attackCooldown > 0f) return;
        attackCooldown = PLAYER_COOLDOWN;
        animationSignals.add(new DirectionalAnimation.Signal(player.getId(), DirectionalAnimation.State.ATTACK));
        Mob closest = null;
        float best = Float.POSITIVE_INFINITY;
        Position origin = player.getPosition();
        attack = new DebugSnapshot.AttackView(origin.x(), origin.y(), facingX, facingY, ATTACK_DURATION);
        for (Mob mob : mobs) {
            if (mob.isDead()) continue;
            Position target = mob.getPosition();
            float dx = target.x() - origin.x();
            float dy = target.y() - origin.y();
            float length = (float) Math.hypot(dx, dy);
            if (length > MELEE_REACH + MOB_RADIUS || length < 0.0001f) continue;
            float dot = (dx * facingX + dy * facingY) / length;
            if (dot < HALF_CONE_COS) continue;
            if (length < best) {
                closest = mob;
                best = length;
            }
        }
        if (closest != null) {
            int amount = combat.attack(player, closest);
            addEffect(closest, amount, false);
            lastEvent = "Golpe desarmado: " + amount + " de dano.";
            if (closest.isDead()) lastEvent = closest.getName() + " derrotado!";
        } else {
            lastEvent = "Golpe no ar: aproxime-se e olhe para o alvo.";
        }
    }

    /** IA mínima: perseguição, colisão contra cenário e ataque corpo a corpo. */
    private void updateEnemies(float dt) {
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob.isDead() || player.isDead()) continue;
            Position p = mob.getPosition();
            Position target = player.getPosition();
            float dx = target.x() - p.x();
            float dy = target.y() - p.y();
            float length = (float) Math.hypot(dx, dy);
            if (length < MOB_AGGRO && length > MOB_MELEE - 2) {
                movement.moveDirected(mob, new Position(dx / length, dy / length),
                        MOB_SPEED, dt, MOB_RADIUS, collisionSpace());
            }
            p = mob.getPosition();
            length = (float) Math.hypot(target.x() - p.x(), target.y() - p.y());
            if (length <= MOB_MELEE && enemyCooldowns[i] <= 0f) {
                enemyCooldowns[i] = MOB_COOLDOWN;
                animationSignals.add(new DirectionalAnimation.Signal(mob.getId(), DirectionalAnimation.State.ATTACK));
                int amount = combat.attack(mob, player);
                addEffect(player, amount, true);
                lastEvent = mob.getName() + " atingiu voce: " + amount + " de dano.";
                if (player.isDead()) lastEvent = "Voce caiu! Pressione R para reiniciar.";
            }
        }
    }

    /** Registra um efeito de dano temporário sem alterar a regra de HP. */
    private void addEffect(GameEntity target, int amount, boolean received) {
        if (amount > 0) animationSignals.add(new DirectionalAnimation.Signal(target.getId(), DirectionalAnimation.State.HIT));
        effects.add(new DebugSnapshot.EffectView(target.getPosition().x(),
                target.getPosition().y() - 25, amount, 0.7f, received));
    }

    // --- Leitura por cópia para a interface ---

    /** Publica apenas valores: nunca a referência mutável do jogador ou dos mobs. */
    public DebugSnapshot snapshot() {
        List<DebugSnapshot.ActorView> actors = new ArrayList<>();
        actors.add(view(player, PLAYER_RADIUS, true));
        for (Mob mob : mobs) actors.add(view(mob, MOB_RADIUS, false));
        HealthComponent hp = player.require(HealthComponent.class);
        float vigor = player.require(StaminaComponent.class).current();
        return new DebugSnapshot(List.copyOf(actors), obstacles.stream().filter(o -> o.category() == CollisionCategory.SOLID_OBSTACLE).map(o ->
                        new DebugSnapshot.Obstacle(o.x(), o.y(), o.width(), o.height())).toList(), List.copyOf(effects),
                hp.current(), hp.max(), defeated, player.isDead(), ticks,
                lastEvent, facingX, facingY, attackCooldown, attack,
                player.require(StaminaComponent.class).current(),
                player.require(StaminaComponent.class).max(), cryostasis.cooldown(),
                cryostasis.zones().stream().map(zone -> new DebugSnapshot.IceZoneView(
                        zone.x(), zone.y(), CryostasisZone.RADIUS, zone.elapsed(), CryostasisZone.DURATION)).toList(),
                mobs.stream().filter(mob -> !mob.isDead()).filter(mob -> mob.require(StatusEffectsComponent.class)
                        .has(StatusEffectType.FREEZE)).map(mob -> new DebugSnapshot.StatusView(mob.getId(),
                        mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE))).toList(),
                lightningStep.active(), lightningStep.remaining(), lightningStep.cooldown(), List.copyOf(lightningTrails),
                lightningStep.awaitingLanding(), softObstacles.stream().map(obstacle -> {
                    var body = obstacle.body();
                    return new DebugSnapshot.SoftObstacleView(body.x(), body.y(), body.width(), body.height(),
                            obstacle.kind());
                }).toList(),
                SkillHudMapper.cryostasis(player.isDead(), vigor, cryostasis.cooldown(), cryostasisFeedback),
                SkillHudMapper.lightningStep(player.isDead(), vigor, lightningStep.cooldown(),
                        lightningStep.active(), lightningStep.awaitingLanding(), lightningStepFeedback));
    }

    /** Extrai apenas os campos visuais de uma entidade do domínio. */
    private static DebugSnapshot.ActorView view(GameEntity entity, float radius, boolean player) {
        HealthComponent hp = entity.require(HealthComponent.class);
        return new DebugSnapshot.ActorView(entity.getId(), entity.getName(),
                entity.getPosition().x(), entity.getPosition().y(), radius,
                hp.current(), hp.max(), player, !hp.isDead());
    }

}
