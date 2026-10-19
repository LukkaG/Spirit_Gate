package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryServiceTest {
    @Test void stacksSplitTransferAndRollbackWithoutLoss() {
        var catalog=new ItemCatalog(RpgContent.builtin()); var inv=new InventoryService(catalog,2);
        assertTrue(inv.add("potion",11)); assertEquals(11,inv.count("potion"));
        var before=inv.snapshot(); assertFalse(inv.add("sword",1)); assertEquals(before,inv.snapshot());
        assertFalse(inv.remove("potion",12)); assertEquals(before,inv.snapshot());
        assertTrue(inv.move(0,1,4)); assertEquals(11,inv.count("potion"));
        assertEquals(inv.snapshot(),InventoryService.restore(catalog,2,inv.snapshot()).snapshot());
        assertThrows(IllegalArgumentException.class,()->inv.add("potion",0));
        assertThrows(IllegalArgumentException.class,()->inv.add("absent",1));
    }
    @Test void worldInventorySurvivesSave() throws Exception {
        RpgWorld world=new RpgWorld(); world.inventory().add("sword",1); world.inventory().move(0,4,1);
        assertEquals(world.save(),RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(world.save()))).save());
    }
}
