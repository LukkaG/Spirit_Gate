package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorujaoVisualStateTest {
    static RpgWorld forest() {
        var s=new RpgWorld().save();
        return RpgWorld.restore(new RpgSave(s.identity(),"forest",240,360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));
    }
    @Test void realAttackHitKoAndRecoveryHavePriorityAndExpire() {
        var w=forest();var visual=new RpgVisualState();visual.update(w.snapshot(),false);
        assertTrue(w.attack());assertEquals(RpgVisualState.State.ATTACK,visual.update(w.snapshot(),false));
        w.receiveDamage(2);assertEquals(RpgVisualState.State.HIT,visual.update(w.snapshot(),true));
        for(int i=0;i<20;i++)assertEquals(RpgVisualState.State.HIT,visual.update(w.snapshot(),true));
        for(int i=0;i<6;i++)w.tick(-1,0);
        assertEquals(RpgVisualState.State.WALK,visual.update(w.snapshot(),true));
        w.receiveDamage(10000);assertEquals(RpgVisualState.State.KO,visual.update(w.snapshot(),false));
        assertTrue(w.returnHome());assertEquals(RpgVisualState.State.IDLE,visual.update(w.snapshot(),false));
    }
    @Test void absentHitAndRepeatedSnapshotsNeverInventActionsOrChangeSaves() {
        var w=new RpgWorld();var before=w.save();var visual=new RpgVisualState();
        assertFalse(w.attack());assertEquals(RpgVisualState.State.IDLE,visual.update(w.snapshot(),false));
        assertEquals(before,w.save());assertTrue(w.snapshot().events().isEmpty());
        for(int i=0;i<70;i++)w.receiveDamage(1);
        assertTrue(w.snapshot().events().size()<=64);
        assertThrows(UnsupportedOperationException.class,()->w.snapshot().events().clear());
    }
}
