package com.projetok.rpg;

import java.util.*;

/** Snapshot persistente V1, copias profundas de colecoes e limites verificaveis. */
public record RpgSave(CharacterIdentity identity, String region, float x, float y, int hp,
                      long xp, int coins, Map<String,Integer> inventory, Map<String,String> equipment,
                      Map<String,Integer> quests, Set<String> flags, long ticks) {
    private static final ItemCatalog ITEMS=new ItemCatalog(RpgContent.builtin());
    public RpgSave {
        Objects.requireNonNull(identity);
        inventory=Map.copyOf(inventory); equipment=Map.copyOf(equipment); quests=Map.copyOf(quests); flags=Set.copyOf(flags);
        RpgContent content=RpgContent.builtin(); content.get("region",region);
        if (!Float.isFinite(x) || !Float.isFinite(y) || x<18 || x>942 || y<18 || y>622 || hp<0 || hp>10000
                || xp<0 || xp>1_000_000 || coins<0 || coins>1_000_000 || ticks<0 || ticks>1_000_000_000L
                || inventory.size()>100 || equipment.size()>8 || quests.size()>64 || flags.size()>2048)
            throw new IllegalArgumentException("Save fora dos limites");
        InventoryService.restore(ITEMS,20,inventory);
        equipment.forEach((slot,id)->{ if(!Set.of("weapon","torso","legs","accessory").contains(slot)) throw new IllegalArgumentException("Slot invalido"); content.get("item",id); });
        quests.forEach((id,q)->{ content.get("quest",id); if(q<0||q>10000) throw new IllegalArgumentException("Quest invalida"); });
        for(String flag:flags) if(!flag.matches("[a-zA-Z0-9_:.-]{1,100}")) throw new IllegalArgumentException("Flag invalida");
    }
}
