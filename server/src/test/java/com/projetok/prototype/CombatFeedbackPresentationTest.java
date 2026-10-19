package com.projetok.prototype;

import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatFeedbackPresentationTest {
    @Test void realDamageEffectsAreImmutableExpireAndRenderingDoesNotApplyDamage() {
        var world=DebugWorld.demo(new Random(5));DebugSnapshot hit=null;
        for(int i=0;i<1200;i++) {
            var s=world.snapshot();var player=s.actors().get(0);
            var mob=s.actors().stream().filter(a->!a.player() && a.alive()).findFirst();
            if(mob.isEmpty())break;
            float dx=mob.get().x()-player.x(),dy=mob.get().y()-player.y();
            world.tick(new DebugInput(dy<-10,dy>10,dx<-10,dx>10,true),1f/30f);
            if(!world.snapshot().effects().isEmpty()) {hit=world.snapshot();break;}
        }
        assertNotNull(hit);var before=world.snapshot();
        assertThrows(UnsupportedOperationException.class,()->before.effects().clear());
        assertThrows(UnsupportedOperationException.class,()->before.actors().clear());
        var panel=new DebugPanel(()->before);var image=new BufferedImage(1260,750,2);var g=image.createGraphics();
        for(int i=0;i<5;i++)panel.render(g,before);g.dispose();assertEquals(before,world.snapshot());
        world.tick(DebugInput.NONE,1f/30f);
        var original=before.effects().get(0);
        assertTrue(world.snapshot().effects().stream().anyMatch(e->e.damage()==original.damage() && e.remaining()<original.remaining()));
        assertEquals(original.remaining(),before.effects().get(0).remaining());
        // Relógio visual continua avançando até após derrota, sem depender do render.
        for(int i=0;i<3000;i++)world.tick(DebugInput.NONE,1f/30f);
        assertTrue(world.snapshot().effects().isEmpty());
    }
}
