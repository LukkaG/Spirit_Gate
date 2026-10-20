package com.projetok.domain.item;

/** Sucessora de Sword.java, generalizada. Arma e armadura são o mesmo tipo de dado, só com campos diferentes preenchidos. */
public record Item(String id, String displayName, ItemType type, int hpBonus, int weaponDamage) {

    public static Item weapon(String id, String displayName, int weaponDamage) {
        return new Item(id, displayName, ItemType.WEAPON, 0, weaponDamage);
    }

    public static Item armor(String id, String displayName, int hpBonus) {
        return new Item(id, displayName, ItemType.ARMOR, hpBonus, 0);
    }

    public static Item misc(String id, String displayName) {
        return new Item(id, displayName, ItemType.MISC, 0, 0);
    }
}
