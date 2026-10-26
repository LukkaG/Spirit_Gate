package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.rpg.RpgWorld;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReaperSpritesTest {
    @Test void allDirectionsRenderEightFramesWithTransparentCorners() throws Exception {
        var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/reaper"),125,1);
        assertEquals(8,sprites.walkFrames());
        for(var direction:Direction.values()) for(int frame=0;frame<8;frame++) {
            var image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
            var g=image.createGraphics();
            try { assertTrue(sprites.drawFrame(g,direction,frame,true,64,100)); }
            finally { g.dispose(); }
            assertEquals(0,image.getRGB(32,40)>>>24);
            assertTrue(sprites.visualBounds(direction,frame,true,64,100).getHeight()>45);
            assertTrue(sprites.visualBounds(direction,frame,true,64,100).getMaxY()<=100);
        }
    }

    @Test void fullWalkCycleReturnsToIdleWithoutChangingWorldRules() throws Exception {
        for(var direction:Direction.values()) {
            var world=new RpgWorld();var control=RpgWorld.restore(world.save());
            var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/reaper"),125,1);
            int dx=direction==Direction.LEFT?-1:direction==Direction.RIGHT?1:0;
            int dy=direction==Direction.BACK?-1:direction==Direction.FRONT?1:0;
            if(dx<0||dy<0) for(int i=0;i<20;i++){world.tick(-dx,-dy);control.tick(-dx,-dy);}
            sprites.update(world.snapshot());Set<Integer> frames=new HashSet<>();
            for(int i=0;i<32;i++) {
                world.tick(dx,dy);control.tick(dx,dy);sprites.update(world.snapshot());
                frames.add(sprites.pose().frame());
                assertEquals(direction,sprites.pose().direction());
                assertEquals(control.save(),world.save());
            }
            assertEquals(Set.of(0,1,2,3,4,5,6,7),frames);
            world.tick(0,0);sprites.update(world.snapshot());
            assertEquals(direction+".0",sprites.frameKey());
        }
    }
}
