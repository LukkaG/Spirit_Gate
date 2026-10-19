package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NpcInteractionTest {
    @Test void contextualRangeAndDeathAreEnforced(){var w=new RpgWorld();assertTrue(w.interact());assertEquals("elder",w.snapshot().activeNpc());for(int i=0;i<25;i++)w.tick(-1,0);assertFalse(w.interact());w.receiveDamage(10000);assertFalse(w.interact());}
}
