package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Detecta e resolve colisões sem interface. Coordenadas e raios usam a mesma unidade. */
public final class CollisionService {
    /** Atrito do auxílio de contorno; movimento livre e deslizamento por eixo não mudam. */
    public static final float SOFT_SLIDE_FACTOR = 0.45f;
    // --- Dados do cenário ---

    /** Obstáculo retangular cuja categoria é compartilhada por todas as políticas. */
    public record Obstacle(float x, float y, float width, float height, CollisionCategory category) {
        /** Conserva a classificação sólida dos cenários e construtores anteriores. */
        public Obstacle(float x, float y, float width, float height) {
            this(x, y, width, height, CollisionCategory.SOLID_OBSTACLE);
        }

        /** Valida o retângulo inclusive quanto a overflow nas extremidades. */
        public Obstacle {
            MovementValidation.finite(x, y, x + width, y + height);
            MovementValidation.positive(width, height);
            Objects.requireNonNull(category, "category");
            if (category != CollisionCategory.SOFT_OBSTACLE && category != CollisionCategory.SOLID_OBSTACLE)
                throw new IllegalArgumentException("Retângulos devem ser obstáculos leves ou sólidos");
        }
    }
    /** Corpo circular; atores sem vida, como NPCs de diálogo, também podem ser sólidos. */
    public record Body(GameEntity entity, float radius) {
        /** Valida a geometria sem exigir componentes específicos de combate. */
        public Body {
            java.util.Objects.requireNonNull(entity);
            MovementValidation.positive(radius);
            MovementValidation.finite(entity.getPosition().x(), entity.getPosition().y());
        }

        /** Corpos circulares de entidades pertencem à categoria de atores. */
        public CollisionCategory category() { return CollisionCategory.ACTOR; }
    }
    /** Listas copiadas, com posições e vida das entidades consultadas ao vivo. */
    public record Space(float width, float height, List<Obstacle> obstacles, List<Body> bodies,
                        SpatialBodyIndex index) {
        /** Contexto sem índice, compatível com mutações diretas das entidades. */
        public Space(float width, float height, List<Obstacle> obstacles, List<Body> bodies) {
            this(width, height, obstacles, bodies, null);
        }
        /** Copia a composição do cenário sem copiar as entidades da simulação. */
        public Space {
            MovementValidation.positive(width, height);
            obstacles = List.copyOf(obstacles);
            bodies = List.copyOf(bodies);
            if (index != null && !index.containsExactly(bodies))
                throw new IllegalArgumentException("Índice deve conter os mesmos corpos do cenário");
        }
    }

    // --- Detecção ---

    /** Tangência é permitida; corpos mortos e o próprio ator não bloqueiam. */
    public boolean isFree(GameEntity entity, float x, float y, float radius, Space space) {
        return isFree(entity, x, y, radius, space, CollisionPolicy.SOLID);
    }

    /** Consulta as categorias bloqueantes da política, preservando tangência e limites. */
    public boolean isFree(GameEntity entity, float x, float y, float radius, Space space,
                          CollisionPolicy policy) {
        MovementValidation.finite(x, y);
        MovementValidation.positive(radius);
        Objects.requireNonNull(policy, "policy");
        if (x < radius || y < radius || x > space.width() - radius
                || y > space.height() - radius) return false;
        for (Obstacle obstacle : space.obstacles()) {
            if (!policy.blocks(obstacle.category())) continue;
            float closestX = clamp(x, obstacle.x(), obstacle.x() + obstacle.width());
            float closestY = clamp(y, obstacle.y(), obstacle.y() + obstacle.height());
            if (overlaps(x, y, closestX, closestY, radius)) return false;
        }
        for (Body body : candidates(space, x, y, x, y, radius)) {
            if (!policy.blocks(body.category())) continue;
            GameEntity other = body.entity();
            if (!bodyBlocks(entity, body)) continue;
            MovementValidation.finite(other.getPosition().x(), other.getPosition().y());
            if (overlaps(x, y, other.getPosition().x(), other.getPosition().y(),
                    (double) radius + body.radius())) return false;
        }
        return true;
    }

    // --- Resolução ---

    /**
     * Resolve X antes de Y, permitindo deslizar pelas paredes. Consulta apenas a
     * posição candidata por eixo, preservando a regra anterior sem varredura contínua.
     * O chamador deve executar na thread proprietária das entidades.
     */
    public void resolve(GameEntity entity, float dx, float dy, float radius, Space space) {
        validateMove(entity, dx, dy, radius);
        Position p = entity.getPosition();
        if (dx != 0 && isFree(entity, p.x() + dx, p.y(), radius, space))
            entity.setPosition(new Position(p.x() + dx, p.y()));
        p = entity.getPosition();
        if (dy != 0 && isFree(entity, p.x(), p.y() + dy, radius, space))
            entity.setPosition(new Position(p.x(), p.y() + dy));
        if (space.index() != null) space.index().update(entity);
    }

    // --- Auxílio de contorno com atrito ---

    /**
     * Preserva X/Y e, somente em bloqueio total, tenta contornar círculos ou quinas
     * externas em velocidade reduzida. Não encolhe hitboxes nem empurra outro ator.
     */
    public void resolveSliding(GameEntity entity, float dx, float dy, float radius, Space space) {
        validateMove(entity, dx, dy, radius);
        Position origin = entity.getPosition();
        if ((dx == 0 && dy == 0) || !isFree(entity, origin.x(), origin.y(), radius, space)) return;
        Position axisX = new Position(origin.x() + dx, origin.y());
        if (dx != 0 && pathIsFree(entity, origin, axisX, radius, space)) entity.setPosition(axisX);
        Position afterX = entity.getPosition();
        Position axisY = new Position(afterX.x(), afterX.y() + dy);
        if (dy != 0 && pathIsFree(entity, afterX, axisY, radius, space)) entity.setPosition(axisY);
        if (!origin.equals(entity.getPosition())) {
            if (space.index() != null) space.index().update(entity);
            return;
        }
        Position best = null;
        for (Obstacle obstacle : space.obstacles()) {
            // Faces planas não escolhem um desvio lateral arbitrário.
            boolean outsideX = origin.x() < obstacle.x() || origin.x() > obstacle.x() + obstacle.width();
            boolean outsideY = origin.y() < obstacle.y() || origin.y() > obstacle.y() + obstacle.height();
            if (!outsideX || !outsideY || !blocksStep(origin, dx, dy, radius, obstacle)) continue;
            double nx = origin.x() - (double) clamp(origin.x(), obstacle.x(), obstacle.x() + obstacle.width());
            double ny = origin.y() - (double) clamp(origin.y(), obstacle.y(), obstacle.y() + obstacle.height());
            best = betterSlide(origin, dx, dy, best,
                    slideCandidate(entity, origin, dx, dy, radius, space, nx, ny));
        }
        for (Body body : candidates(space, origin.x(), origin.y(), origin.x() + dx, origin.y() + dy, radius)) {
            if (!bodyBlocks(entity, body)) continue;
            Position other = body.entity().getPosition();
            double sum = radius + (double) body.radius();
            double squared = sum * sum;
            if (pointSegmentSquared(other.x(), other.y(), origin, axisX) >= squared
                    && pointSegmentSquared(other.x(), other.y(), origin, new Position(origin.x(), origin.y() + dy)) >= squared
                    && pointSegmentSquared(other.x(), other.y(), origin, new Position(origin.x() + dx, origin.y() + dy)) >= squared) continue;
            best = betterSlide(origin, dx, dy, best,
                    slideCandidate(entity, origin, dx, dy, radius, space,
                            origin.x() - (double) other.x(), origin.y() - (double) other.y()));
        }
        if (best != null) {
            entity.setPosition(best);
            if (space.index() != null) space.index().update(entity);
        }
    }

    /** Identifica a quina que rejeitou pelo menos uma das tentativas do passo original. */
    private boolean blocksStep(Position p, float dx, float dy, float radius, Obstacle o) {
        return obstacleBlocksSegment(p, new Position(p.x() + dx, p.y()), radius, o)
                || obstacleBlocksSegment(p, new Position(p.x(), p.y() + dy), radius, o)
                || obstacleBlocksSegment(p, new Position(p.x() + dx, p.y() + dy), radius, o);
    }

    /** Consulta de um círculo contra um único retângulo, com a mesma regra de tangência. */
    private boolean overlapsRectangle(float x, float y, float radius, Obstacle o) {
        return overlaps(x, y, clamp(x, o.x(), o.x() + o.width()),
                clamp(y, o.y(), o.y() + o.height()), radius);
    }

    /** Projeta na tangente e aplica atrito, recuando o passo quando houver outro bloqueio. */
    private Position slideCandidate(GameEntity entity, Position origin, float dx, float dy,
                                    float radius, Space space, double nx, double ny) {
        double normalLength = Math.hypot(nx, ny);
        if (normalLength == 0) return null;
        nx /= normalLength; ny /= normalLength;
        double inward = dx * nx + dy * ny;
        if (inward >= 0) return null;
        double sx = dx - inward * nx, sy = dy - inward * ny;
        double length = Math.hypot(sx, sy);
        if (length < 0.000001) return null; // contato frontal simétrico não escolhe um lado.
        double scale = Math.min(SOFT_SLIDE_FACTOR, radius * 0.25 / length);
        for (int i = 0; i < 4; i++, scale *= 0.5) {
            Position end = new Position((float) (origin.x() + sx * scale), (float) (origin.y() + sy * scale));
            if (!Float.isFinite(end.x()) || !Float.isFinite(end.y())) continue;
            if (end.equals(origin)) continue;
            if (pathIsFree(entity, origin, end, radius, space)) return end;
        }
        return null;
    }

    /** Seleciona maior progresso na intenção; desempate geométrico evita oscilação pela ordem do índice. */
    private Position betterSlide(Position origin, float dx, float dy, Position best, Position candidate) {
        if (candidate == null) return best;
        double score = (candidate.x() - (double) origin.x()) * dx + (candidate.y() - (double) origin.y()) * dy;
        if (score <= 0) return best;
        if (best == null) return candidate;
        double previous = (best.x() - (double) origin.x()) * dx + (best.y() - (double) origin.y()) * dy;
        if (score > previous + 0.000000001 || (Math.abs(score - previous) <= 0.000000001
                && (candidate.x() < best.x() || (candidate.x() == best.x() && candidate.y() < best.y())))) return candidate;
        return best;
    }

    /** Valida o segmento inteiro da assistência, incluindo terceiros corpos e cantos arredondados. */
    private boolean pathIsFree(GameEntity entity, Position start, Position end, float radius, Space space) {
        if (!isFree(entity, end.x(), end.y(), radius, space)) return false;
        for (Obstacle o : space.obstacles()) {
            if (obstacleBlocksSegment(start, end, radius, o)) return false;
        }
        for (Body body : candidates(space, start.x(), start.y(), end.x(), end.y(), radius)) {
            if (!bodyBlocks(entity, body)) continue;
            var p = body.entity().getPosition();
            double sum = radius + (double) body.radius();
            if (pointSegmentSquared(p.x(), p.y(), start, end) < sum * sum) return false;
        }
        return true;
    }

    /** Distância mínima do segmento ao retângulo, incluindo faces e quinas. */
    private boolean obstacleBlocksSegment(Position start, Position end, float radius, Obstacle o) {
        double squared = (double) radius * radius;
        return overlapsRectangle(start.x(), start.y(), radius, o)
                || overlapsRectangle(end.x(), end.y(), radius, o)
                || segmentIntersectsRectangle(start, end, o)
                || pointSegmentSquared(o.x(), o.y(), start, end) < squared
                || pointSegmentSquared(o.x() + o.width(), o.y(), start, end) < squared
                || pointSegmentSquared(o.x(), o.y() + o.height(), start, end) < squared
                || pointSegmentSquared(o.x() + o.width(), o.y() + o.height(), start, end) < squared;
    }

    /** Teste de segmento contra o interior retangular pela interseção dos intervalos X/Y. */
    private boolean segmentIntersectsRectangle(Position start, Position end, Obstacle o) {
        double near = 0, far = 1;
        double[] origins = {start.x(), start.y()}, deltas = {(double) end.x() - start.x(), (double) end.y() - start.y()};
        double[] low = {o.x(), o.y()}, high = {o.x() + o.width(), o.y() + o.height()};
        for (int axis = 0; axis < 2; axis++) {
            if (deltas[axis] == 0) {
                if (origins[axis] < low[axis] || origins[axis] > high[axis]) return false;
            } else {
                double a = (low[axis] - origins[axis]) / deltas[axis];
                double b = (high[axis] - origins[axis]) / deltas[axis];
                near = Math.max(near, Math.min(a, b)); far = Math.min(far, Math.max(a, b));
                if (near > far) return false;
            }
        }
        return true;
    }

    /** Distância de ponto ao segmento, incluindo as duas extremidades. */
    private double pointSegmentSquared(double x, double y, Position start, Position end) {
        double dx = (double) end.x() - start.x(), dy = (double) end.y() - start.y();
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1,
                ((x - start.x()) * dx + (y - start.y()) * dy) / lengthSquared));
        double rx = x - (start.x() + t * dx), ry = y - (start.y() + t * dy);
        return rx * rx + ry * ry;
    }

    /** Valida ambos os eixos e seus resultados antes de aplicar o primeiro. */
    private void validateMove(GameEntity entity, float dx, float dy, float radius) {
        Position p = entity.getPosition();
        MovementValidation.finite(p.x(), p.y(), dx, dy, p.x() + dx, p.y() + dy);
        MovementValidation.positive(radius);
    }

    /** Escolhe a busca espacial quando disponível; contexto simples permanece compatível. */
    private List<Body> candidates(Space space, float x0, float y0, float x1, float y1, float radius) {
        return space.index() == null ? space.bodies()
                : space.index().query(Math.min(x0, x1) - (double) radius,
                        Math.min(y0, y1) - (double) radius,
                        Math.max(x0, x1) + (double) radius,
                        Math.max(y0, y1) + (double) radius);
    }

    /** Ignora o próprio ator e mortos; NPCs sem componente de vida continuam bloqueantes. */
    private boolean bodyBlocks(GameEntity entity, Body body) {
        return body.entity() != entity && !body.entity().get(HealthComponent.class)
                .map(HealthComponent::isDead).orElse(false);
    }

    // --- Varredura contínua opcional ---

    /**
     * Varre cada eixo até o primeiro contato, X antes de Y. Exige origem livre.
     * Não substitui a resolução discreta da arena nem altera sua mecânica atual.
     */
    public void resolveContinuous(GameEntity entity, float dx, float dy, float radius, Space space) {
        resolveContinuous(entity, dx, dy, radius, space, CollisionPolicy.SOLID);
    }

    /** Varre X/Y com categorias seletivas; a política nunca autoriza atravessar limites. */
    public void resolveContinuous(GameEntity entity, float dx, float dy, float radius, Space space,
                                  CollisionPolicy policy) {
        validateMove(entity, dx, dy, radius);
        Objects.requireNonNull(policy, "policy");
        Position p = entity.getPosition();
        if (!isFree(entity, p.x(), p.y(), radius, space, policy))
            throw new IllegalStateException("Varredura exige posição inicial livre");
        float x = sweepAxis(entity, p.x(), p.y(), dx, radius, space, true, policy);
        entity.setPosition(new Position(x, p.y()));
        float y = sweepAxis(entity, p.y(), x, dy, radius, space, false, policy);
        entity.setPosition(new Position(x, y));
        if (space.index() != null) space.index().update(entity);
    }

    /** Usa os intervalos geométricos comuns à varredura e à recuperação da trajetória. */
    private float sweepAxis(GameEntity entity, float origin, float fixed, float delta,
                            float radius, Space space, boolean horizontal, CollisionPolicy policy) {
        if (delta == 0) return origin;
        double target = Math.max(radius, Math.min((double) origin + delta,
                (horizontal ? space.width() : space.height()) - (double) radius));
        for (Interval interval : axisIntervals(entity, origin, (float) target, fixed, radius,
                space, horizontal, policy))
            target = stopAtInterval(origin, target, interval.low(), interval.high());
        float result = (float) target;
        // Contato arredondado para dentro é recuado alguns ULPs, sem tolerância de penetração.
        for (int i = 0; i < 8; i++) {
            if (isFree(entity, horizontal ? result : fixed, horizontal ? fixed : result,
                    radius, space, policy)) return result;
            result = Math.nextAfter(result, origin);
        }
        return origin;
    }

    /** Intervalo aberto ocupado pelo centro do círculo; suas extremidades permitem tangência. */
    private record Interval(double low, double high) { }

    /** Calcula uma única vez a geometria analítica de retângulos arredondados e círculos. */
    private List<Interval> axisIntervals(GameEntity entity, float origin, float target, float fixed,
                                         float radius, Space space, boolean horizontal,
                                         CollisionPolicy policy) {
        List<Interval> intervals = new ArrayList<>();
        for (Obstacle o : space.obstacles()) {
            if (!policy.blocks(o.category())) continue;
            double low = horizontal ? o.x() : o.y();
            double high = low + (horizontal ? o.width() : o.height());
            double crossLow = horizontal ? o.y() : o.x();
            double crossHigh = crossLow + (horizontal ? o.height() : o.width());
            double distance = Math.max(0, Math.max(crossLow - fixed, fixed - crossHigh));
            if (distance >= radius) continue;
            double extension = Math.sqrt((double) radius * radius - distance * distance);
            intervals.add(new Interval(low - extension, high + extension));
        }
        float x0 = horizontal ? origin : fixed, y0 = horizontal ? fixed : origin;
        float x1 = horizontal ? target : fixed, y1 = horizontal ? fixed : target;
        for (Body body : candidates(space, x0, y0, x1, y1, radius)) {
            if (!policy.blocks(body.category())) continue;
            if (!bodyBlocks(entity, body)) continue;
            Position p = body.entity().getPosition();
            MovementValidation.finite(p.x(), p.y());
            double center = horizontal ? p.x() : p.y();
            double distance = Math.abs((horizontal ? p.y() : p.x()) - (double) fixed);
            double sum = (double) radius + body.radius();
            if (distance >= sum) continue;
            double extension = Math.sqrt(sum * sum - distance * distance);
            intervals.add(new Interval(center - extension, center + extension));
        }
        return intervals;
    }

    /** Interrompe na entrada do intervalo aberto, permitindo tangência. */
    private double stopAtInterval(double origin, double target, double low, double high) {
        if (origin > low && origin < high) return origin;
        if (target > origin && origin <= low && target > low) return low;
        if (target < origin && origin >= high && target < high) return high;
        return target;
    }

    // --- Recuperação após travessia seletiva ---

    /** Resultado imutável, com o caminho reverso efetivamente usado pela correção. */
    public record Recovery(boolean recovered, Position position, List<Position> path) {
        /** Copia os vértices para impedir alterações pelos consumidores. */
        public Recovery {
            Objects.requireNonNull(position, "position");
            path = List.copyOf(path);
        }
    }

    /**
     * Procura a posição normalmente livre mais próxima do destino na rota real, em ordem reversa.
     * Cada trecho deve ser alinhado a um eixo, incluindo os vértices produzidos pelo deslizamento.
     * A geometria atual é consultada: atores podem ter mudado de posição desde o início do dash.
     * Paredes interrompem o recuo. Sem solução, preserva a posição e permite nova tentativa posterior.
     * Toda a rota é validada antes de qualquer mutação; a correção atualiza o índice espacial.
     */
    public Recovery recoverAlongPath(GameEntity entity, List<Position> travelled,
                                     float radius, Space space) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(space, "space");
        MovementValidation.positive(radius);
        List<Position> route = List.copyOf(travelled);
        if (route.isEmpty()) throw new IllegalArgumentException("Trajetória não pode ser vazia");
        Position current = entity.getPosition();
        MovementValidation.finite(current.x(), current.y());
        for (int i = 0; i < route.size(); i++) {
            Position point = route.get(i);
            MovementValidation.finite(point.x(), point.y());
            if (i > 0) {
                Position previous = route.get(i - 1);
                MovementValidation.finite(point.x() - previous.x(), point.y() - previous.y());
                if (point.x() != previous.x() && point.y() != previous.y())
                    throw new IllegalArgumentException("Trajetória deve conter os trechos reais por eixo");
            }
        }
        if (!current.equals(route.get(route.size() - 1)))
            throw new IllegalArgumentException("Trajetória deve terminar na posição atual");
        List<Position> untouched = List.of(current);
        if (isFree(entity, current.x(), current.y(), radius, space))
            return new Recovery(true, current, untouched);
        CollisionPolicy selective = CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT;
        if (!isFree(entity, current.x(), current.y(), radius, space, selective))
            return new Recovery(false, current, untouched);

        List<Position> reversePath = new ArrayList<>();
        reversePath.add(current);
        Position from = current;
        for (int i = route.size() - 2; i >= 0; i--) {
            Position desired = route.get(i);
            if (desired.equals(from)) continue;
            boolean horizontal = from.y() == desired.y();
            float origin = horizontal ? from.x() : from.y();
            float target = horizontal ? desired.x() : desired.y();
            float fixed = horizontal ? from.y() : from.x();
            float reachable = sweepAxis(entity, origin, fixed, target - origin,
                    radius, space, horizontal, selective);
            Position free = firstFreeOnAxis(entity, origin, reachable, fixed, radius, space, horizontal);
            if (free != null) {
                if (!free.equals(reversePath.get(reversePath.size() - 1))) reversePath.add(free);
                entity.setPosition(free);
                if (space.index() != null) space.index().update(entity);
                return new Recovery(true, free, reversePath);
            }
            // Uma parede surgida no caminho impede usar vértices além dela.
            if (reachable != target) break;
            from = desired;
            reversePath.add(from);
        }
        return new Recovery(false, current, untouched);
    }

    /**
     * Examina os limites analíticos dos intervalos ocupados, sem amostragem de distância.
     * A ordem preserva o primeiro ponto livre, inclusive tangências entre intervalos encostados.
     */
    private Position firstFreeOnAxis(GameEntity entity, float origin, float target, float fixed,
                                      float radius, Space space, boolean horizontal) {
        if (origin == target) return null;
        List<Double> boundaries = new ArrayList<>();
        double low = Math.min(origin, target), high = Math.max(origin, target);
        for (Interval interval : axisIntervals(entity, origin, target, fixed, radius,
                space, horizontal, CollisionPolicy.SOLID)) {
            if (interval.low() >= low && interval.low() <= high) boundaries.add(interval.low());
            if (interval.high() >= low && interval.high() <= high) boundaries.add(interval.high());
        }
        boundaries.add((double) target);
        boundaries.sort(target > origin ? Comparator.naturalOrder() : Comparator.reverseOrder());
        for (double boundary : boundaries) {
            float coordinate = (float) boundary;
            // Ajusta apenas arredondamento de contato, na direção do espaço procurado.
            for (int i = 0; i < 8; i++) {
                if (coordinate < low || coordinate > high) break;
                float x = horizontal ? coordinate : fixed, y = horizontal ? fixed : coordinate;
                if (isFree(entity, x, y, radius, space)) return new Position(x, y);
                float next = Math.nextAfter(coordinate, target);
                if (next == coordinate) break;
                coordinate = next;
            }
        }
        return null;
    }

    // --- Geometria ---

    /** Limita uma coordenada ao intervalo do obstáculo. */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Calcula distância ao quadrado sem raiz quadrada. */
    private static float distanceSquared(float ax, float ay, float bx, float by) {
        float dx = ax - bx, dy = ay - by;
        return dx * dx + dy * dy;
    }

    /** Preserva cálculo histórico quando possível e evita overflow em magnitudes extremas. */
    private boolean overlaps(float x, float y, float otherX, float otherY, double radius) {
        float distance = distanceSquared(x, y, otherX, otherY);
        float legacyRadius = (float) radius;
        float threshold = legacyRadius * legacyRadius;
        if (Float.isFinite(distance) && Float.isFinite(threshold) && threshold > 0)
            return distance < threshold;
        double dx = (double) x - otherX, dy = (double) y - otherY;
        return dx * dx + dy * dy < radius * radius;
    }
}
