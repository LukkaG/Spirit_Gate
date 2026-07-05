package com.projetok.model;

public class CombatCalculator {

    public static int calculateDamage(Entity attacker, Entity target) {
        double strengthBonus = (attacker.getStrength() / 5.0) * 2.5;
        int weaponDamage = attacker.getWeaponDamage();

        return (int) (strengthBonus + weaponDamage);
    }

}
