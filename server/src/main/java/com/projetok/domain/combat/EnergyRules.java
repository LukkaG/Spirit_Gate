package com.projetok.domain.combat;

/** Energia em décimos: 100 unidades e 15 unidades/s antes dos investimentos. */
public final class EnergyRules {
    public static final int SCALE=10,BASE_MAXIMUM=1000,BASE_REGEN_PER_TICK=5;
    private EnergyRules(){}
    private static void validate(int energy){if(energy<0||energy>10000)throw new IllegalArgumentException("Energy inválido");}
    public static int maximum(int energy){validate(energy);return BASE_MAXIMUM+energy*20;}
    public static int regenerationPermille(int energy){validate(energy);return 1000+Math.min(energy,100)*10;}
    public static double regenerationPerTick(int energy){return BASE_REGEN_PER_TICK*regenerationPermille(energy)/1000.0;}
    public static double regenerationPerSecond(int energy){return regenerationPerTick(energy)*30/SCALE;}
}
