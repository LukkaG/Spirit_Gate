package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TradePresentationTest {
    @Test void rejectedTradePreservesMoneyAndInventoryAndUsesSharedQuotes(){
        var state=new RpgWorld().save();
        var world=RpgWorld.restore(new RpgSave(state.identity(),"village",230,400,state.hp(),0,100,state.inventory(),state.equipment(),state.quests(),state.flags(),0));
        assertTrue(world.interact());assertEquals("trader",world.snapshot().activeNpc());
        var catalog=new ItemCatalog(RpgContent.builtin());assertEquals(12,RpgWorld.tradePrice(catalog.get("potion"),true));
        for(int i=0;i<19;i++)assertTrue(world.inventory().add("sword",1));
        var before=world.save();assertFalse(world.trade("robe",1,true));assertEquals(before,world.save());
        assertFalse(world.trade("sword",99,true));assertEquals(before,world.save());
        assertThrows(IllegalArgumentException.class,()->world.trade("absent",1,true));
        assertThrows(IllegalArgumentException.class,()->world.trade("potion",0,true));assertEquals(before,world.save());
        assertTrue(RpgWorld.merchantItems().contains("sword"));assertThrows(UnsupportedOperationException.class,()->RpgWorld.merchantItems().clear());
    }
}
