package com.projetok.domain.entity.component;

import com.projetok.domain.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class InventoryComponent implements Component {

    private final List<Item> items = new ArrayList<>();

    public void add(Item item) {
        items.add(item);
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    public List<Item> items() {
        return List.copyOf(items);
    }
}
