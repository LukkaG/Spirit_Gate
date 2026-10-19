package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LootServiceTest {
    @Test void generationLedgerAndPendingDropsPreventLossOrDuplication() {
        var content=RpgContent.builtin(); var items=new ItemCatalog(content); var loot=new LootService(content,items);
        var inv=new InventoryService(items,1); inv.add("sword",1); Set<String> flags=new TreeSet<>();
        assertTrue(loot.award("forest_0",1,"slime",inv,flags,new Random(4)));
        var before=Set.copyOf(flags); assertFalse(loot.award("forest_0",1,"slime",inv,flags,new Random(4))); assertEquals(before,flags);
        assertFalse(LootService.pending(flags).isEmpty()); inv.remove("sword",1); loot.collect(inv,flags);
        assertTrue(inv.count("crystal")>0); assertTrue(LootService.pending(flags).isEmpty());
        assertTrue(loot.award("forest_0",2,"slime",inv,flags,new Random(4)));
        assertFalse(loot.award("forest_0",1,"slime",inv,flags,new Random(4)));
        assertThrows(IllegalArgumentException.class,()->new LootService.Drop("crystal",Double.NaN,1,2));
    }
}
