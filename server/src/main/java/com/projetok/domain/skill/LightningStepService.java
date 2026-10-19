package com.projetok.domain.skill;

import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.CollisionPolicy;
import com.projetok.domain.movement.MovementService;
import com.projetok.domain.resource.StaminaService;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * Controla o Passo Relâmpago de um jogador na thread proprietária da simulação.
 * Não causa dano, não aplica efeitos e não depende de renderização ou rede.
 */
public final class LightningStepService {
    /** Identificador aceito pelo contrato de intenção de habilidades. */
    public static final String ID = "lightning_step";
    /** Distância máxima por ativação, em unidades de mundo. */
    public static final float MAX_DISTANCE = 150f;
    /** Duração do deslocamento, em segundos, mesmo quando há bloqueios. */
    public static final float DURATION = 0.25f;
    /** Custo provisório de vigor físico. */
    public static final float COST = 35f;
    /** Intervalo mínimo entre ativações, em segundos. */
    public static final float COOLDOWN = 4f;
    /** Velocidade constante antes da resolução das colisões. */
    public static final float SPEED = MAX_DISTANCE / DURATION;
    private static final double CLOCK_TOLERANCE = 0.000001;
    private final MovementService movement;
    private final StaminaService stamina;
    private final CollisionService collisions;
    private final CollisionPolicy policy;
    private final List<Position> travelled = new ArrayList<>();
    private boolean awaitingLanding;
    private Position direction = new Position(0f, 0f);
    private double remaining;
    private double cooldown;

    /** Motivo da aceitação ou recusa, sem efeitos colaterais nas recusas. */
    public enum Status { STARTED, INVALID, DEAD, ZERO_DIRECTION, COOLDOWN, STAMINA, LANDING_PENDING }

    /** Trajeto imutável do tick, incluindo curvas por eixo e eventual recuo de saída. */
    public record Step(Position from, Position to, List<Position> path, boolean blocked) {
        /** Garante que as duas extremidades estejam disponíveis ao consumidor. */
        public Step {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            path = List.copyOf(path);
        }

        /** Preserva consumidores anteriores que não publicavam informação de contato. */
        public Step(Position from, Position to, List<Position> path) {
            this(from, to, path, false);
        }

        /** Preserva o construtor anterior, explicitando o vértice intermediário X/Y. */
        public Step(Position from, Position to) {
            this(from, to, List.of(from, new Position(to.x(), from.y()), to));
        }

        /** Informa se houve deslocamento efetivo após a resolução das colisões. */
        public boolean moved() { return path.stream().anyMatch(point -> !point.equals(from)); }
    }

    /** Injeta os serviços existentes, mantendo um único caminho para mover e consumir. */
    public LightningStepService(MovementService movement, StaminaService stamina,
                                CollisionService collisions) {
        this(movement, stamina, collisions, CollisionPolicy.SOLID);
    }

    /** Seleciona os contatos da habilidade; o construtor antigo preserva a colisão sólida. */
    public LightningStepService(MovementService movement, StaminaService stamina,
                                CollisionService collisions, CollisionPolicy policy) {
        this.movement = Objects.requireNonNull(movement, "movement");
        this.stamina = Objects.requireNonNull(stamina, "stamina");
        this.collisions = Objects.requireNonNull(collisions, "collisions");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /** Informa se ainda resta tempo de deslocamento desta ativação. */
    public boolean active() { return remaining > 0; }

    /** Aguarda saída livre quando corpos móveis ocuparam toda a trajetória do dash. */
    public boolean awaitingLanding() { return awaitingLanding; }

    /** Retorna os segundos restantes de deslocamento. */
    public float remaining() { return (float) remaining; }

    /** Retorna os segundos restantes até uma nova ativação. */
    public float cooldown() { return (float) cooldown; }

    /** Retorna a direção imutável fixada no lançamento, sem acompanhar o cursor. */
    public Position direction() { return direction; }

    // --- Elegibilidade e início sem teleporte ---

    /**
     * Valida o comando e a origem antes de consumir vigor ou iniciar o cooldown.
     * O mouse define somente a direção: o orçamento é de até 150 unidades,
     * inclusive quando o cursor está próximo. O primeiro avanço ocorre em update.
     */
    public Status cast(Player player, SkillCastCommand command, float radius,
                       CollisionService.Space space) {
        if (player == null || command == null || space == null || !ID.equals(command.skillId())
                || !Float.isFinite(radius) || radius <= 0f
                || !Float.isFinite(command.targetX()) || !Float.isFinite(command.targetY())
                || command.targetX() < 0f || command.targetY() < 0f
                || command.targetX() >= space.width() || command.targetY() >= space.height())
            return Status.INVALID;
        if (player.isDead()) return Status.DEAD;
        if (awaitingLanding) return Status.LANDING_PENDING;
        Position origin = player.getPosition();
        if (!Float.isFinite(origin.x()) || !Float.isFinite(origin.y())
                || !collisions.isFree(player, origin.x(), origin.y(), radius, space))
            return Status.INVALID;
        double dx = (double) command.targetX() - origin.x();
        double dy = (double) command.targetY() - origin.y();
        double length = Math.hypot(dx, dy);
        if (length == 0) return Status.ZERO_DIRECTION;
        if (cooldown > 0) return Status.COOLDOWN;
        if (!stamina.tryConsume(player, COST)) return Status.STAMINA;
        direction = new Position((float) (dx / length), (float) (dy / length));
        remaining = DURATION;
        cooldown = COOLDOWN;
        travelled.clear();
        travelled.add(origin);
        return Status.STARTED;
    }

    // --- Relógio e deslocamento contínuo ---

    /**
     * Avança o cooldown e, quando ativo, move pelo menor tempo entre dt e o restante.
     * Colisões usam a varredura X/Y existente; um passo bloqueado consome seu tempo
     * sem compensar a distância depois. A morte interrompe somente o deslocamento.
     * Delta inválido é rejeitado antes de qualquer mutação; zero é permitido.
     * Ao terminar, recupera uma saída normalmente livre pela trajetória percorrida.
     * Se todos os pontos estiverem ocupados, aguarda e tenta novamente sem renovar
     * duração, consumo ou cooldown. O chamador deve suspender WASD enquanto aguarda.
     */
    public Step update(Player player, float dt, float radius, CollisionService.Space space) {
        if (!Float.isFinite(dt) || dt < 0f)
            throw new IllegalArgumentException("Delta deve ser finito e não negativo");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(space, "space");
        if (!Float.isFinite(radius) || radius <= 0f)
            throw new IllegalArgumentException("Raio deve ser finito e positivo");
        Position from = player.getPosition();
        List<Position> tickPath = new ArrayList<>();
        tickPath.add(from);
        boolean blocked = false;
        cooldown = Math.max(0, cooldown - dt);
        if (cooldown < CLOCK_TOLERANCE) cooldown = 0;
        if (player.isDead()) {
            remaining = 0;
            awaitingLanding = false;
            travelled.clear();
        } else if (active() && dt > 0f) {
            double stepTime = Math.min((double) dt, remaining);
            float distance = (float) (SPEED * stepTime);
            movement.moveContinuous(player, direction.x() * distance, direction.y() * distance,
                    radius, space, policy);
            Position to = player.getPosition();
            // Compara o avanço real antes do recuo de saída; o cursor não comprova contato.
            blocked = shortened(from.x(), to.x(), direction.x() * distance)
                    || shortened(from.y(), to.y(), direction.y() * distance);
            appendAxisPath(tickPath, from, to);
            appendAxisPath(travelled, from, to);
            remaining = Math.max(0, remaining - stepTime);
            if (!active()) awaitingLanding = true;
        }
        // A saída considera as posições atuais dos corpos, sem prolongar o relógio do dash.
        if (awaitingLanding) {
            var recovery = collisions.recoverAlongPath(player, travelled, radius, space);
            if (recovery.recovered()) {
                for (Position point : recovery.path()) appendDistinct(tickPath, point);
                awaitingLanding = false;
                travelled.clear();
            }
        }
        return new Step(from, player.getPosition(), tickPath, blocked);
    }

    /** Desconsidera somente o arredondamento natural das posições em ponto flutuante. */
    private static boolean shortened(float from, float to, float requested) {
        float tolerance = Math.max(0.0001f,
                Math.max(Math.ulp(from), Math.ulp(to)) * 4f);
        return Math.abs(requested) - Math.abs(to - from) > tolerance;
    }

    /** Conserva a curva efetiva da varredura X/Y, sem substituir por uma corda diagonal. */
    private static void appendAxisPath(List<Position> path, Position from, Position to) {
        appendDistinct(path, new Position(to.x(), from.y()));
        appendDistinct(path, to);
    }

    /** Descarta vértices repetidos sem arredondar as posições autoritativas. */
    private static void appendDistinct(List<Position> path, Position point) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(point)) path.add(point);
    }
}
