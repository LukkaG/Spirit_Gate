package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DungeonCycleTest {
    static void fight(RpgWorld w,int ticks){for(int i=0;i<ticks;i++){w.attack();if(w.save().hp()<w.maxHp()-45)w.use("potion");w.tick(0,0);}assertTrue(w.save().hp()>0);}
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"guardian","mystic"}) void fullDungeonFromNewCharacterWithoutInjectedProgress(String archetype) {
        var w=new RpgWorld(CharacterIdentity.create("Expedicao",archetype,"amber"));w.interact();w.acceptQuest("first_steps");OfflineCycleTest.move(w,855,320);w.interact();fight(w,450);OfflineCycleTest.move(w,80,320);w.interact();OfflineCycleTest.move(w,190,320);w.interact();assertTrue(w.completeQuest("first_steps"));assertTrue(w.acceptQuest("sanctuary"));
        OfflineCycleTest.move(w,330,440);w.harvest("starter_chest");assertTrue(w.equip("sword"));OfflineCycleTest.move(w,855,320);w.interact();OfflineCycleTest.move(w,855,320);assertTrue(w.interact());assertEquals("ruins",w.save().region());
        OfflineCycleTest.move(w,270,420);fight(w,120);OfflineCycleTest.move(w,510,420);fight(w,160);OfflineCycleTest.move(w,670,320);fight(w,260);assertTrue(w.save().flags().contains("dungeon-completed"));assertEquals(1,w.inventory().count("relic"));
        w=RpgWorld.restore(w.save());OfflineCycleTest.move(w,150,350);OfflineCycleTest.move(w,80,320);assertTrue(w.interact());OfflineCycleTest.move(w,80,320);assertTrue(w.interact());OfflineCycleTest.move(w,190,320);w.interact();assertTrue(w.completeQuest("sanctuary"));assertTrue(w.save().flags().contains("quest-done:sanctuary"));
    }
}
