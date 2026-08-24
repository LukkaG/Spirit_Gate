package com.projetok.domain.ai;

import com.projetok.domain.entity.GameEntity;

import java.util.Optional;

/**
 * Marca o alvo hostil mais próximo dentro do alcance. A decisão de efetivamente atacar
 * (chamar CombatService) fica por conta de um futuro MobAiSystem que itera as entidades com
 * AiComponent a cada tick do GameLoop — isso mantém o Component livre de depender de
 * CombatService diretamente. Um sistema de IA completo (pathing, tabela de aggro etc.) está
 * fora do escopo deste redesenho.
 */
public final class AggressiveBehavior implements AiBehavior {

    private static final double DEFAULT_AGGRO_RANGE = 8.0;

    private final double aggroRange;
    private GameEntity currentTarget;

    public AggressiveBehavior() {
        this(DEFAULT_AGGRO_RANGE);
    }

    public AggressiveBehavior(double aggroRange) {
        this.aggroRange = aggroRange;
    }

    @Override
    public void tick(GameEntity self, GameWorldView world) {
        currentTarget = world.findNearestHostile(self, aggroRange).orElse(null);
    }

    public Optional<GameEntity> currentTarget() {
        return Optional.ofNullable(currentTarget);
    }
}
