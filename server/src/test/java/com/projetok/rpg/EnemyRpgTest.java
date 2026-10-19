package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnemyRpgTest {
    static RpgWorld forest() { var s=new RpgWorld().save();return RpgWorld.restore(new RpgSave(s.identity(),"forest",240,360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks())); }
    @Test void regionSpawnDeathRewardsAndRespawn() {
        assertTrue(new RpgWorld().snapshot().enemies().isEmpty());var w=forest();assertEquals(3,w.snapshot().enemies().size());
        for(int i=0;i<3;i++){assertTrue(w.attack());for(int t=0;t<12;t++)w.tick(0,0);}
        assertEquals(20,w.save().xp());assertEquals(5,w.save().coins());assertEquals(0,w.snapshot().enemies().get(0).hp());
        assertFalse(w.attack());assertEquals(20,w.save().xp());
        var restored=RpgWorld.restore(w.save());assertEquals(w.save(),restored.save());
        for(int t=0;t<250;t++)restored.tick(0,0);assertEquals(40,restored.snapshot().enemies().get(0).hp());
    }
}
