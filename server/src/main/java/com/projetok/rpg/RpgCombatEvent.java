package com.projetok.rpg;

/** Evento efemero de combate confirmado pelo mundo; nao integra o formato de save. */
public record RpgCombatEvent(long sequence,long tick,Kind kind,int target,float x,float y,int amount) {
    /** Natureza real da acao ou consequencia; REJECT nunca representa dano. */
    public enum Kind { ATTACK, HIT, HEAL, KO, REJECT }
}
