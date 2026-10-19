package com.projetok.rpg;
/** Ciclo de 2400 ticks. Somente apresentacao; nunca altera combate. */
public final class EnvironmentCycle {
    private EnvironmentCycle() { }
    public static float light(long tick,boolean enabled) {
        if(tick<0)throw new IllegalArgumentException("Tick negativo");
        if(!enabled)return 1;
        double phase=(tick%2400)/2400.0;
        return (float)(0.91+0.09*Math.cos(phase*Math.PI*2));
    }
}
