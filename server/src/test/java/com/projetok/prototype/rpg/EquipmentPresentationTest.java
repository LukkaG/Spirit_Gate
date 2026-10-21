package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EquipmentPresentationTest {
    @Test void comparisonUsesActualSlotPowerAndDoesNotEquip(){
        var world=new RpgWorld();var catalog=new ItemCatalog(RpgContent.builtin());var sword=catalog.get("sword");
        var before=world.save();assertTrue(InventoryWindow.comparison(world.snapshot(),sword).contains("+"+sword.power()));assertEquals(before,world.save());
        world.inventory().add("sword",1);assertTrue(world.equip("sword"));
        assertTrue(InventoryWindow.comparison(world.snapshot(),sword).contains("+0"));
        assertEquals(world.save(),RpgWorld.restore(world.save()).save());assertTrue(world.unequip("weapon"));
        assertTrue(InventoryWindow.comparison(world.snapshot(),catalog.get("potion")).contains("Não ocupa"));
    }
}
