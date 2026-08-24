package com.projetok.domain.entity.capability;

import com.projetok.domain.item.Item;

import java.util.List;

public interface Lootable {
    List<Item> rollLoot();
}
