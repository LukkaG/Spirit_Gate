package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import com.projetok.prototype.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CorujaoAttackPresentationTest {
    @Test void successiveAttacksExpireAndDamageInterruptsWithoutBlockingMovement() throws Exception {
        var w=CorujaoVisualStateTest.forest();var s=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250);s.update(w.snapshot());
        assertTrue(w.attack());s.update(w.snapshot());assertEquals(RpgVisualState.State.ATTACK,s.visualState());
        var save=w.save();var image=new java.awt.image.BufferedImage(300,300,2);var g=image.createGraphics();
        for(int i=0;i<50;i++)s.draw(g,new DebugSnapshot.ActorView(1,"Test",140,220,18,save.hp(),w.maxHp(),true,true));
        g.dispose();assertEquals(save,w.save());assertFalse(w.attack());
        w.receiveDamage(1);s.update(w.snapshot());assertEquals(RpgVisualState.State.HIT,s.visualState());
        float x=w.snapshot().x();for(int i=0;i<12;i++){w.tick(-1,0);s.update(w.snapshot());}
        assertTrue(w.snapshot().x()<x);assertEquals(RpgVisualState.State.WALK,s.visualState());
        w.receiveDamage(10000);s.update(w.snapshot());assertEquals(RpgVisualState.State.KO,s.visualState());
        assertTrue(w.returnHome());s.update(w.snapshot());assertEquals(RpgVisualState.State.IDLE,s.visualState());
    }
}
