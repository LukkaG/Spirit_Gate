package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorldObjectsTest {
    @Test void chestPersistsAndDoesNotDuplicate(){var w=new RpgWorld();assertFalse(w.harvest("starter_chest"));OfflineCycleTest.move(w,330,440);assertTrue(w.harvest("starter_chest"));assertEquals(1,w.inventory().count("sword"));assertFalse(w.harvest("starter_chest"));var restored=RpgWorld.restore(w.save());assertFalse(restored.harvest("starter_chest"));assertEquals(w.save(),restored.save());}
}
