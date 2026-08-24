package com.projetok.domain.entity.component;

import com.projetok.domain.item.EquipmentSlot;
import com.projetok.domain.item.Item;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Fonte única de "o que está equipado" — CombatComponent lê o dano da arma daqui, nunca guarda a própria. */
public final class EquipmentComponent implements Component {

    private final Map<EquipmentSlot, Item> equipped = new EnumMap<>(EquipmentSlot.class);

    public void equip(EquipmentSlot slot, Item item) {
        equipped.put(slot, item);
    }

    public Optional<Item> unequip(EquipmentSlot slot) {
        return Optional.ofNullable(equipped.remove(slot));
    }

    public Optional<Item> equippedAt(EquipmentSlot slot) {
        return Optional.ofNullable(equipped.get(slot));
    }

    public int totalHpBonus() {
        return equipped.values().stream().mapToInt(Item::hpBonus).sum();
    }

    public int weaponDamage() {
        return equippedAt(EquipmentSlot.WEAPON).map(Item::weaponDamage).orElse(0);
    }
}
