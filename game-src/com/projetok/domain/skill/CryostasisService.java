package com.projetok.domain.skill;

import com.projetok.domain.combat.CombatService;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.resource.StaminaService;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.probability.ProbabilityService;
import java.util.random.RandomGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Estado da Criostase de um lançador, usado exclusivamente pela thread da simulação. */
public final class CryostasisService {
    public static final String ID = "cryostasis";
    public static final float COST = 55f, COOLDOWN = 3f, RANGE = 240f;
    /** Chance por dano positivo sobrevivido; sucessos renovam um segundo. */
    public static final double FREEZE_CHANCE = 0.30;
    public static final float FREEZE_DURATION = 1f;
    private final CombatService combat;
    private final StaminaService stamina;
    private final StatusEffectService statusEffects;
    private final ProbabilityService probability;
    private final CryostasisAttacker attacker = new CryostasisAttacker();
    private final List<CryostasisZone> zones = new ArrayList<>();
    private double cooldown;

    /** Alvo circular da arena; vida e posição são consultadas novamente a cada pulso. */
    public record Target(Mob mob, float radius) {
        /** Valida a hitbox antes de entrar na simulação. */
        public Target {
            Objects.requireNonNull(mob);
            if (!Float.isFinite(radius) || radius <= 0) throw new IllegalArgumentException("Raio inválido");
        }
    }
    /** Resultado de dano aplicado somente por CombatService. */
    public record Hit(Mob mob, int damage) { }
    /** Motivo explícito da aceitação ou recusa do comando. */
    public enum Status { CAST, INVALID, DEAD, COOLDOWN, STAMINA }
    /** Resultado imutável para o coordenador publicar feedback. */
    public record Result(Status status, List<Hit> hits) {
        /** Impede alteração da coleção pelo consumidor. */
        public Result { hits = List.copyOf(hits); }
    }

    /** Recebe os serviços existentes, sem criar eventos ou caminhos paralelos de dano. */
    public CryostasisService(CombatService combat, StaminaService stamina) {
        this(combat, stamina, new StatusEffectService(), new ProbabilityService(RandomGenerator.getDefault()));
    }

    /** Injeta aplicação de efeitos e sorteios reproduzíveis sem modificar o caminho de dano. */
    public CryostasisService(CombatService combat, StaminaService stamina,
                            StatusEffectService statusEffects, ProbabilityService probability) {
        this.combat = Objects.requireNonNull(combat); this.stamina = Objects.requireNonNull(stamina);
        this.statusEffects = Objects.requireNonNull(statusEffects);
        this.probability = Objects.requireNonNull(probability);
    }
    /** Cooldown restante, publicável sem referência mutável. */
    public float cooldown() { return (float) cooldown; }
    /** Cópia da composição; o snapshot visual deve extrair os valores das zonas. */
    public List<CryostasisZone> zones() { return List.copyOf(zones); }

    // --- Elegibilidade e lançamento ---

    /** Valida antes de consumir; ponto válido fora do alcance é limitado na mesma direção. */
    public Result cast(Player player, SkillCastCommand command, float width, float height, List<Target> targets) {
        if (command == null || !ID.equals(command.skillId()) || !Float.isFinite(command.targetX())
                || !Float.isFinite(command.targetY()) || !Float.isFinite(width) || !Float.isFinite(height)
                || width <= 0 || height <= 0 || command.targetX() < 0 || command.targetY() < 0
                || command.targetX() >= width || command.targetY() >= height)
            return new Result(Status.INVALID, List.of());
        if (player.isDead()) return new Result(Status.DEAD, List.of());
        if (cooldown > 0) return new Result(Status.COOLDOWN, List.of());
        var origin = player.getPosition();
        if (!Float.isFinite(origin.x()) || !Float.isFinite(origin.y())) return new Result(Status.INVALID, List.of());
        double dx = (double) command.targetX() - origin.x(), dy = (double) command.targetY() - origin.y();
        double distance = Math.hypot(dx, dy);
        float x = command.targetX(), y = command.targetY();
        if (distance > RANGE) {
            x = (float) (origin.x() + dx / distance * RANGE);
            y = (float) (origin.y() + dy / distance * RANGE);
        }
        if (!stamina.tryConsume(player, COST)) return new Result(Status.STAMINA, List.of());
        cooldown = COOLDOWN;
        var zone = new CryostasisZone(x, y);
        zones.add(zone);
        return new Result(Status.CAST, pulse(zone, zone.advance(0), targets));
    }

    // --- Relógio e pulsos ---

    /** Avança zonas mesmo após morte do lançador; novas ações continuam bloqueadas. */
    public List<Hit> update(float dt, List<Target> targets) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Delta inválido");
        cooldown = Math.max(0, cooldown - dt);
        if (cooldown < 0.000001) cooldown = 0;
        List<Hit> hits = new ArrayList<>();
        for (var zone : zones) hits.addAll(pulse(zone, zone.advance(dt), targets));
        zones.removeIf(CryostasisZone::expired);
        return List.copyOf(hits);
    }

    /** Cada alvo vivo recebe no máximo uma aplicação por pulso, inclusive em tangência. */
    private List<Hit> pulse(CryostasisZone zone, int count, List<Target> targets) {
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Set<Mob> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Target target : targets) {
                Mob mob = target.mob();
                if (!visited.add(mob) || mob.isDead()) continue;
                var p = mob.getPosition();
                double dx = (double) p.x() - zone.x(), dy = (double) p.y() - zone.y();
                double radius = CryostasisZone.RADIUS + (double) target.radius();
                if (dx * dx + dy * dy <= radius * radius) {
                    int damage = combat.attack(attacker, mob);
                    hits.add(new Hit(mob, damage));
                    // Sorteio somente após dano positivo e sobrevivência, independente por alvo/pulso.
                    if (damage > 0 && !mob.isDead() && probability.succeeds(FREEZE_CHANCE))
                        statusEffects.apply(mob, StatusEffectType.FREEZE, FREEZE_DURATION);
                }
            }
        }
        return hits;
    }
}
