package com.projetok.rpg;
import com.projetok.domain.movement.CollisionService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoBossThreatTest {
    @Test void preparationTimeAndExecutionMatchRealBoss() {
        var w=BossEncounterTest.ruins();w.tick(0,0);
        var boss=w.snapshot().enemies().stream().filter(EnemyService.View::boss).findFirst().orElseThrow();
        assertNotNull(boss.threat());assertEquals(110,boss.threat().radius());assertEquals(30,boss.threat().remainingTicks());
        assertEquals(boss.threat(),RpgWorld.restore(w.save()).snapshot().enemies().stream().filter(EnemyService.View::boss).findFirst().orElseThrow().threat());
        int hp=w.save().hp();for(int i=0;i<29;i++)w.tick(0,0);assertEquals(hp,w.save().hp());
        w.tick(0,0);assertEquals(hp-14,w.save().hp());
        assertNull(w.snapshot().enemies().stream().filter(EnemyService.View::boss).findFirst().orElseThrow().threat());
    }
    @Test void warningRespectsRangeAndTheSameWallOcclusionAsCombat() {
        var geometry=new CollisionService.Space(960,640,java.util.List.of(new CollisionService.Obstacle(380,180,120,120)),java.util.List.of());
        var t=new RpgBossThreat(519,260,110,18,false,geometry);
        assertFalse(t.contains(460,319));assertTrue(t.contains(550,320));assertFalse(t.contains(700,320));
    }
}
