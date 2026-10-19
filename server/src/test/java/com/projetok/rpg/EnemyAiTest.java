package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnemyAiTest {
    @Test void perceivesAttacksAndRestoresDeterministically() {
        var w=EnemyRpgTest.forest();for(int i=0;i<40;i++)w.tick(0,0);
        assertTrue(w.save().hp()<w.maxHp());assertEquals("ATTACK",w.snapshot().enemies().get(0).state());
        var copy=RpgWorld.restore(w.save());for(int i=0;i<30;i++){w.tick(-1,0);copy.tick(-1,0);}assertEquals(w.save(),copy.save());
        w.receiveDamage(10000);int deadHp=w.save().hp();for(int i=0;i<30;i++)w.tick(0,0);assertEquals(deadHp,w.save().hp());
    }
    @Test void obstaclesNeverContainEnemies() {
        var w=EnemyRpgTest.forest();for(int i=0;i<160;i++)w.tick(0,-1);
        for(var e:w.snapshot().enemies())assertFalse(e.x()>361&&e.x()<519&&e.y()>161&&e.y()<319);
    }
}
