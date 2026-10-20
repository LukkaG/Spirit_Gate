package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorujaoFeedbackTest {
    @Test void numbersMatchActualDamageAndHealingAndDoNotRepeat() {
        var w=CorujaoVisualStateTest.forest();var f=new RpgCombatFeedback();f.update(w.snapshot());
        int before=w.snapshot().enemies().get(0).hp();assertTrue(w.attack());f.update(w.snapshot());
        int delta=before-w.snapshot().enemies().get(0).hp();
        assertEquals(delta,f.active().stream().filter(e->e.kind()==RpgCombatEvent.Kind.HIT).mapToInt(RpgCombatEvent::amount).sum());
        int count=f.active().size();for(int i=0;i<100;i++)f.update(w.snapshot());assertEquals(count,f.active().size());
        w.receiveDamage(3);assertTrue(w.use("potion"));f.update(w.snapshot());
        assertEquals(3,f.active().stream().filter(e->e.kind()==RpgCombatEvent.Kind.HEAL).mapToInt(RpgCombatEvent::amount).sum());
        for(int i=0;i<30;i++)w.tick(-1,0);f.update(w.snapshot());assertTrue(f.active().isEmpty());
    }
    @Test void rejectedAttackHasNoFalseDamageAndDrawingIsPure() {
        var w=new RpgWorld();assertFalse(w.activate("rpg.attack"));var f=new RpgCombatFeedback();f.update(w.snapshot());
        assertEquals(1,f.active().size());assertEquals(RpgCombatEvent.Kind.REJECT,f.active().get(0).kind());
        var state=w.save();var image=new java.awt.image.BufferedImage(400,400,2);var g=image.createGraphics();
        for(int i=0;i<100;i++)f.draw(g);g.dispose();assertEquals(state,w.save());assertEquals(1,f.active().size());
    }
}
