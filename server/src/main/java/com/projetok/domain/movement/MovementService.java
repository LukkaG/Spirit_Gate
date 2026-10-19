package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.effect.StatusEffectType;

/** Movimento autoritativo sem interface; chamado na thread proprietária das entidades. */
public final class MovementService {
    /** Política explícita: referência por eixos ou assistência lenta no contorno. */
    public enum Response { STRICT, SOFT_SLIDE }
    private final CollisionService collisions;
    private final Response response;

    /** Recebe o serviço responsável pela detecção e resolução de colisões. */
    public MovementService(CollisionService collisions) {
        this(collisions, Response.STRICT);
    }

    /** Seleciona a resposta à colisão sem modificar velocidade, raios ou combate. */
    public MovementService(CollisionService collisions, Response response) {
        this.collisions = java.util.Objects.requireNonNull(collisions);
        this.response = java.util.Objects.requireNonNull(response);
    }

    /** Retorna direção unitária, ou zero quando não há intenção de movimento. */
    public Position direction(float x, float y) {
        MovementValidation.finite(x, y);
        if (x == 0f && y == 0f) return new Position(0f, 0f);
        double length = Math.hypot(x, y);
        // Preserva arredondamento histórico em magnitudes representáveis como float.
        if (length >= Float.MIN_NORMAL && length <= Float.MAX_VALUE) {
            float legacyLength = (float) length;
            return new Position(x / legacyLength, y / legacyLength);
        }
        return new Position((float) (x / length), (float) (y / length));
    }

    /** Calcula deslocamento de uma direção já normalizada, preservando a ordem das operações. */
    public void moveDirected(GameEntity entity, Position direction, float speed, float dt,
                             float radius, CollisionService.Space space) {
        MovementValidation.finite(direction.x(), direction.y());
        MovementValidation.nonNegative(speed, dt);
        move(entity, direction.x() * speed * dt, direction.y() * speed * dt, radius, space);
    }

    /** Encaminha um deslocamento à resolução de colisões, sem criar threads. */
    public void move(GameEntity entity, float dx, float dy, float radius, CollisionService.Space space) {
        if (blockedByFreeze(entity, dx, dy, radius)) return;
        if (response == Response.SOFT_SLIDE) collisions.resolveSliding(entity, dx, dy, radius, space);
        else collisions.resolve(entity, dx, dy, radius, space);
    }

    /** Deslocamento contínuo opcional para dash e velocidades altas, sem tunelamento. */
    public void moveContinuous(GameEntity entity, float dx, float dy, float radius,
                               CollisionService.Space space) {
        moveContinuous(entity, dx, dy, radius, space, CollisionPolicy.SOLID);
    }

    /** Aplica uma política de contato à mesma varredura contínua, sem alterar o WASD. */
    public void moveContinuous(GameEntity entity, float dx, float dy, float radius,
                               CollisionService.Space space, CollisionPolicy policy) {
        java.util.Objects.requireNonNull(policy, "policy");
        if (blockedByFreeze(entity, dx, dy, radius)) return;
        collisions.resolveContinuous(entity, dx, dy, radius, space, policy);
    }

    /** Freeze bloqueia só mobs e preserva rejeição de entradas inválidas, mesmo imobilizados. */
    private boolean blockedByFreeze(GameEntity entity, float dx, float dy, float radius) {
        if (!(entity instanceof Mob) || !entity.get(StatusEffectsComponent.class)
                .map(component -> component.has(StatusEffectType.FREEZE)).orElse(false)) return false;
        Position p = entity.getPosition();
        MovementValidation.finite(p.x(), p.y(), dx, dy, p.x() + dx, p.y() + dy);
        MovementValidation.positive(radius);
        return true;
    }
}
