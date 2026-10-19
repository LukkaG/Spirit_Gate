package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PortalTest {
    @Test void travelReturnsAndPreservesProgress() {
        var w=new RpgWorld();assertFalse(w.travel("village_east"));for(int i=0;i<105;i++)w.tick(1,0);assertTrue(w.interact());assertEquals("forest",w.save().region());assertEquals(3,w.inventory().count("potion"));
        for(int i=0;i<30;i++)w.tick(0,0);for(int i=0;i<5;i++)w.tick(-1,0);assertTrue(w.interact());assertEquals("village",w.save().region());assertEquals(w.save(),RpgWorld.restore(w.save()).save());
    }
}
