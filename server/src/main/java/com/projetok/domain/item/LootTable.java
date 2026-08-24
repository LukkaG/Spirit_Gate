package com.projetok.domain.item;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootTable {

    public record LootEntry(Item item, double dropChance) {
        public LootEntry {
            if (dropChance <= 0.0 || dropChance > 1.0) {
                throw new IllegalArgumentException("dropChance deve estar entre 0 (exclusivo) e 1 (inclusivo)");
            }
        }
    }

    private final List<LootEntry> entries;

    private LootTable(List<LootEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    /** Conveniência para itens garantidos (100% de chance cada). */
    public static LootTable of(Item... guaranteedItems) {
        Builder builder = builder();
        for (Item item : guaranteedItems) {
            builder.add(item, 1.0);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Item> roll() {
        List<Item> dropped = new ArrayList<>();
        for (LootEntry entry : entries) {
            if (ThreadLocalRandom.current().nextDouble() < entry.dropChance()) {
                dropped.add(entry.item());
            }
        }
        return dropped;
    }

    public static final class Builder {
        private final List<LootEntry> entries = new ArrayList<>();

        public Builder add(Item item, double dropChance) {
            entries.add(new LootEntry(item, dropChance));
            return this;
        }

        public LootTable build() {
            return new LootTable(entries);
        }
    }
}
