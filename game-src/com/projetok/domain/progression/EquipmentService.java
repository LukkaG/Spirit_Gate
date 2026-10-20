package com.projetok.domain.progression;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.item.EquipmentSlot;
import com.projetok.domain.item.Item;

import java.util.Objects;

public final class EquipmentService {

    private final VitalityService vitalityService;

    public EquipmentService(VitalityService vitalityService) {
        this.vitalityService = Objects.requireNonNull(vitalityService, "vitalityService");
    }

    public void equip(GameEntity entity, EquipmentSlot slot, Item item) {
        entity.require(EquipmentComponent.class).equip(slot, item);
        vitalityService.recalculate(entity); // HP máximo muda na hora, sem esperar o próximo level up
    }

    public void unequip(GameEntity entity, EquipmentSlot slot) {
        entity.require(EquipmentComponent.class).unequip(slot);
        vitalityService.recalculate(entity);
    }
}
