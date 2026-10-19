package com.projetok.domain.movement;

/** Validação compartilhada nas fronteiras dos serviços de movimento. */
final class MovementValidation {
    private MovementValidation() { }

    /** Recusa números não finitos antes de qualquer mutação. */
    static void finite(float... values) {
        for (float value : values)
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Movimento exige números finitos");
    }

    /** Recusa valores negativos e não finitos. */
    static void nonNegative(float... values) {
        finite(values);
        for (float value : values)
            if (value < 0) throw new IllegalArgumentException("Valor deve ser não negativo");
    }

    /** Recusa dimensões ou raios nulos, negativos e não finitos. */
    static void positive(float... values) {
        finite(values);
        for (float value : values)
            if (value <= 0) throw new IllegalArgumentException("Dimensão deve ser positiva");
    }
}
