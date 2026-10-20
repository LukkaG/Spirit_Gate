package com.projetok.domain.race;

/** Bônus automático de atributo por nível, específico da raça (ex.: fraqueza elemental compensada aqui). */
public record AttributeGrowth(int strength, int intelligence, int agility) {

    public static final AttributeGrowth NONE = new AttributeGrowth(0, 0, 0);
}
