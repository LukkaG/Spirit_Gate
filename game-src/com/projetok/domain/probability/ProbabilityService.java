package com.projetok.domain.probability;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Sorteios reutilizáveis com gerador Java 17 injetável e limites explícitos. */
public final class ProbabilityService {
    private final RandomGenerator random;

    /** Recebe o gerador pertencente à thread da simulação ou controlado por um teste. */
    public ProbabilityService(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Testa uma chance finita entre zero e um, usando comparação estrita da amostra.
     * Chances zero e um são determinísticas e não consomem o gerador.
     */
    public boolean succeeds(double chance) {
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0)
            throw new IllegalArgumentException("Probabilidade deve ser finita e estar entre zero e um");
        if (chance == 0.0) return false;
        if (chance == 1.0) return true;
        double sample = random.nextDouble();
        if (!Double.isFinite(sample) || sample < 0.0 || sample >= 1.0)
            throw new IllegalStateException("Amostra do gerador deve ser finita e estar em [0, 1)");
        return sample < chance;
    }
}
