package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TradeTest {
    @Test void moneyAndInventoryCommitTogether() {
        var s=new RpgWorld().save();var w=RpgWorld.restore(new RpgSave(s.identity(),"village",230,400,s.hp(),s.xp(),100,s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));w.interact();assertEquals("trader",w.snapshot().activeNpc());
        assertTrue(w.trade("sword",1,true));assertEquals(65,w.save().coins());assertTrue(w.equip("sword"));assertFalse(w.trade("sword",1,false));
        var before=w.save();assertFalse(w.trade("sword",3,true));assertEquals(before,w.save());assertFalse(w.trade("relic",1,false));
        w.inventory().add("crystal",2);assertTrue(w.trade("crystal",2,false));assertEquals(67,w.save().coins());assertEquals(w.save(),RpgWorld.restore(w.save()).save());
    }
}
