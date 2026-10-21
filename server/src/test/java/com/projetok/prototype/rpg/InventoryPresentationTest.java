package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryPresentationTest {
    @Test void itemDetailsAndCapacityFollowRealInventory() {
        var catalog=new ItemCatalog(RpgContent.builtin());var inventory=new InventoryService(catalog,20);
        assertTrue(InventoryWindow.inventoryStatus(inventory.snapshot()).contains("vazio"));
        for(int i=0;i<20;i++)assertTrue(inventory.add("sword",1));
        assertTrue(InventoryWindow.inventoryStatus(inventory.snapshot()).contains("cheio"));assertFalse(inventory.add("sword",1));
        var item=catalog.get("sword");assertTrue(InventoryWindow.describe(item).contains("Poder: "+item.power()));
        assertTrue(inventory.remove("sword",1));assertTrue(InventoryWindow.inventoryStatus(inventory.snapshot()).contains("19 / 20"));
    }
}
