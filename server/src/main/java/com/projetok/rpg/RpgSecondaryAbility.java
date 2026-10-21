package com.projetok.rpg;

/** Metadados da tecnica livre defensiva, compartilhada por todas as racas. */
public record RpgSecondaryAbility(String name,int range,int bonus,int cost,int cooldown) {
    /** Compatibilidade de apresentacao; nao concede habilidades de dominio ao nascer. */
    public static RpgSecondaryAbility forArchetype(String id) {
        CharacterRaces.get(CharacterRaces.normalizeLegacy(id));
        return new RpgSecondaryAbility("Shunpo defensivo",70,0,RpgWorld.SHUNPO_COST,RpgWorld.SHUNPO_COOLDOWN);
    }
}
