package com.projetok.domain.resource;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;

/** Controla vigor sem interface ou rede, na thread proprietária das entidades. */
public final class StaminaService {
    /** Taxa provisória do protótipo em pontos por segundo. */
    public static final float REGENERATION_PER_SECOND = 15f;

    // --- Elegibilidade e consumo ---

    /**
     * Verifica vida e saldo antes de consumir. O chamador só executa a ação quando
     * retornar true. Custo inválido lança exceção mesmo para entidade morta.
     */
    public boolean tryConsume(GameEntity entity, float cost) {
        validateNonNegative(cost);
        if (entity.require(HealthComponent.class).isDead()) return false;
        return entity.require(StaminaComponent.class).tryConsume(cost);
    }

    // --- Regeneração ---

    /** Regenera por delta de tempo finito e não negativo; mortos não regeneram. */
    public void regenerate(GameEntity entity, float dt) {
        validateNonNegative(dt);
        if (entity.require(HealthComponent.class).isDead()) return;
        StaminaComponent stamina = entity.require(StaminaComponent.class);
        // Double evita overflow na multiplicação de deltas finitos muito grandes.
        float amount = (float) Math.min(stamina.max(), (double) REGENERATION_PER_SECOND * dt);
        stamina.restore(amount);
    }

    /** Rejeita valores inválidos antes de consultar ou modificar o recurso. */
    private static void validateNonNegative(float amount) {
        if (!Float.isFinite(amount) || amount < 0f)
            throw new IllegalArgumentException("Valor deve ser finito e não negativo");
    }
}
