package com.projetok.model;

public class CombatCalculator {

    public static int calculateDamage(Human attacker, Entity target) {
        int baseDamage = Human.getDamage() + Sword.getdamage();
    }
}
