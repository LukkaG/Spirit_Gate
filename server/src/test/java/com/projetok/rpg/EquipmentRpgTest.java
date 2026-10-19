package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EquipmentRpgTest {
    @Test void equipsRecalculatesAndRollsBackFullInventory() {
        var w=new RpgWorld(); int base=w.attackPower();
        assertFalse(w.equip("potion")); assertFalse(w.equip("sword"));
        w.inventory().add("sword",1); assertTrue(w.equip("sword")); assertEquals(base+8,w.attackPower());
        assertEquals(w.attackPower(),RpgWorld.restore(w.save()).attackPower());
        assertTrue(w.inventory().add("robe",19)); var before=w.save();
        assertFalse(w.unequip("weapon")); assertEquals(before,w.save());
        w.inventory().remove("robe",1); assertTrue(w.unequip("weapon")); assertEquals(base,w.attackPower());
    }
}
