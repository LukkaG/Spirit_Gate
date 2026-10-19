package com.projetok.domain.entity.component;

/** Vigor físico fracionário, sempre finito e no intervalo de zero ao máximo. */
public final class StaminaComponent implements Component {
    private final float maximum;
    private float current;

    /** Cria o recurso cheio com o valor provisório de 100 pontos. */
    public StaminaComponent() {
        this(100f);
    }

    /** Cria o recurso cheio; o máximo deve ser finito e estritamente positivo. */
    public StaminaComponent(float maximum) {
        if (!Float.isFinite(maximum) || maximum <= 0f)
            throw new IllegalArgumentException("Vigor máximo deve ser finito e positivo");
        this.maximum = maximum;
        this.current = maximum;
    }

    /** Retorna o vigor disponível. */
    public float current() { return current; }

    /** Retorna o limite do recurso. */
    public float max() { return maximum; }

    /** Consome somente se houver saldo; custo inválido lança exceção sem mutação. */
    public boolean tryConsume(float cost) {
        validateAmount(cost);
        if (cost > current) return false;
        current -= cost;
        return true;
    }

    /** Restaura uma quantidade finita e não negativa, sem ultrapassar o máximo. */
    public void restore(float amount) {
        validateAmount(amount);
        current = (float) Math.min(maximum, (double) current + amount);
    }

    /** Valida uma quantidade antes de qualquer alteração de estado. */
    private static void validateAmount(float amount) {
        if (!Float.isFinite(amount) || amount < 0f)
            throw new IllegalArgumentException("Quantidade de vigor deve ser finita e não negativa");
    }
}
