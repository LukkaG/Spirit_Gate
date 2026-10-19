package com.projetok.domain.skill;

/** Zona fixa, com relógio determinístico e cinco pulsos consumidos uma única vez. */
public final class CryostasisZone {
    private final float x, y;
    private double elapsed;
    private int nextPulse;
    /** Duração da zona, independente do último pulso. */
    public static final float DURATION = 2.5f;
    /** Raio da zona em unidades de mundo. */
    public static final float RADIUS = 85f;

    /** Cria zona com centro finito; o primeiro advance(0) libera o pulso imediato. */
    public CryostasisZone(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Centro inválido");
        this.x = x; this.y = y;
    }
    /** Centro horizontal fixo. */
    public float x() { return x; }
    /** Centro vertical fixo. */
    public float y() { return y; }
    /** Tempo publicado para animação e expiração. */
    public float elapsed() { return (float) elapsed; }
    /** Indica expiração, com tolerância apenas para arredondamento dos deltas float. */
    public boolean expired() { return elapsed + 0.000001 >= DURATION; }

    /** Avança o relógio e retorna quantos pulsos novos são devidos; nunca libera um sexto. */
    public int advance(float dt) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Delta inválido");
        elapsed = Math.min(DURATION, elapsed + dt);
        int count = 0;
        while (nextPulse < 5 && nextPulse * 0.5 <= elapsed + 0.000001) {
            nextPulse++; count++;
        }
        return count;
    }
}
