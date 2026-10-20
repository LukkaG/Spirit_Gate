package com.projetok.rpg;

/** Habilidade adicional experimental; compartilha recarga persistente com a habilidade primaria. */
public record RpgSecondaryAbility(String name,int range,int bonus,int cost,int cooldown) {
    /** Parametros conservadores dos dois arquetipos; custos em pontos de vigor. */
    public static RpgSecondaryAbility forArchetype(String id) {
        return switch(id) {
            case "guardian"->new RpgSecondaryAbility("Golpe de Impacto",82,6,20,120);
            case "mystic"->new RpgSecondaryAbility("Pulso Arcano",110,4,25,150);
            default->throw new IllegalArgumentException("Arquetipo desconhecido");
        };
    }
}
