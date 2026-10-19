package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConsumableTest {
    @Test void invalidUsesNeverConsumeAndCooldownSurvivesReload() {
        var w=new RpgWorld(); assertFalse(w.use("potion")); assertEquals(3,w.inventory().count("potion"));
        w.receiveDamage(60); assertTrue(w.use("potion")); assertEquals(2,w.inventory().count("potion"));
        var loaded=RpgWorld.restore(w.save()); assertFalse(loaded.use("potion"));
        for(int i=0;i<60;i++) loaded.tick(0,0);
        assertTrue(loaded.use("potion")); assertEquals(loaded.maxHp(),loaded.save().hp());
        loaded.receiveDamage(10000); var before=loaded.save(); assertFalse(loaded.use("potion")); assertEquals(before,loaded.save());
    }
}
