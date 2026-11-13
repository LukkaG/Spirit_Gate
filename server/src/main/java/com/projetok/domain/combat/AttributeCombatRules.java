package com.projetok.domain.combat;

/** Coeficientes de atributos aprovados; não recebem resultados calculados pelo cliente. */
public final class AttributeCombatRules {
    public enum DamageKind { BASIC, UNARMED_SKILL, DOMAIN_SKILL, OTHER }
    private AttributeCombatRules(){}
    public static int damageBonus(DamageKind kind,int strength,int intelligence){if(kind==null||strength<0||intelligence<0)throw new IllegalArgumentException("Atributos inválidos");return switch(kind){case BASIC,UNARMED_SKILL->strength/2;case DOMAIN_SKILL->intelligence/2;case OTHER->0;};}
    public static double basicSpeedMultiplier(int agility){if(agility<0)throw new IllegalArgumentException("Agilidade inválida");return 1+Math.min(agility,100)/100.0;}
    public static double basicDodgeChance(int agility){if(agility<0)throw new IllegalArgumentException("Agilidade inválida");return Math.min(.25,agility*.005);}
    public static double basicInterval(int baseTicks,int agility){if(baseTicks<1)throw new IllegalArgumentException("Intervalo inválido");return baseTicks/basicSpeedMultiplier(agility);}
    /** Conserva a fração entre ataques consecutivos, sem acumular crédito durante ociosidade. */
    public static double nextBasicReady(double previous,long now,int baseTicks,int agility){if(now<0||!Double.isNaN(previous)&&(!Double.isFinite(previous)||previous>now))throw new IllegalArgumentException("Cadência inválida");double anchor=Double.isNaN(previous)||previous<now-1?now:previous;return anchor+basicInterval(baseTicks,agility);}
}
