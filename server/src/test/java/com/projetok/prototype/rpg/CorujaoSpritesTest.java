package com.projetok.prototype.rpg;

import com.projetok.rpg.RpgWorld;
import com.projetok.prototype.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CorujaoSpritesTest {
    @Test void fourFramesPerDirectionLoopAt250msAndIdleKeepsFeet() throws Exception {
        for(var direction:DirectionalAnimation.Direction.values()) {
            var world=new RpgWorld();var control=RpgWorld.restore(world.save());
            var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250);
            sprites.update(world.snapshot());Set<Integer> frames=new HashSet<>();
            int dx=direction==DirectionalAnimation.Direction.LEFT?-1:direction==DirectionalAnimation.Direction.RIGHT?1:0;
            int dy=direction==DirectionalAnimation.Direction.BACK?-1:direction==DirectionalAnimation.Direction.FRONT?1:0;
            // Reposition through movement, then observe a full cycle before reaching the boundary.
            if(dx<0||dy<0) for(int i=0;i<20;i++){world.tick(-dx,-dy);control.tick(-dx,-dy);}
            sprites.update(world.snapshot());
            for(int i=0;i<32;i++) {
                world.tick(dx,dy);control.tick(dx,dy);sprites.update(world.snapshot());
                frames.add(sprites.pose().frame());
                assertEquals(control.save(),world.save());
                assertEquals(direction,sprites.pose().direction());
                var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(1,"Test",128,220,18,10,10,true,true)));g.dispose();
                int bottom=-1;for(int y=0;y<256;y++)for(int x=0;x<256;x++)if((image.getRGB(x,y)>>>24)!=0)bottom=Math.max(bottom,y);
                assertTrue(bottom>=218&&bottom<=220);
            }
            assertEquals(Set.of(0,1,2,3),frames);
            world.tick(0,0);sprites.update(world.snapshot());assertEquals(direction+".0",sprites.frameKey());
        }
    }
}
