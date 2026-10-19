package com.projetok.prototype;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CullingTest {
    @Test void optimizedPixelsEqualReferenceAtZoomResizeAndMapEdges() {
        var map=LargeMapTest.large();var world=DebugWorld.forMap(map,new Random(1));var snapshot=world.snapshot();
        for(int zoom=1;zoom<=4;zoom++)for(int[] size:new int[][]{{1260,750},{800,600},{1800,950}}) {
            var panel=new DebugPanel(()->snapshot);panel.setLocalMap(map);panel.setSize(size[0],size[1]);
            panel.setCameraZoom(zoom);panel.setShowHitboxes(true);
            var a=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);
            var b=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);
            var ga=a.createGraphics();var gb=b.createGraphics();
            panel.setCulling(false);panel.render(ga,snapshot);
            panel.setCulling(true);panel.render(gb,snapshot);ga.dispose();gb.dispose();
            assertArrayEquals(a.getRGB(0,0,size[0],size[1],null,0,size[0]),b.getRGB(0,0,size[0],size[1],null,0,size[0]));
            assertEquals(snapshot,world.snapshot());
        }
    }
    @Test void visibleRegionAndPartialDiagonalIntersection() {
        var v=ArenaViewport.fit(1260,750,1920,1280,1500,1000,2);
        var r=v.visibleWorld();assertEquals(480,r.width);assertEquals(320,r.height);
        assertTrue(r.intersects(r.getMaxX()-1,r.getMaxY()-1,32,32));
        assertFalse(r.intersects(r.getMaxX()+1,r.getMaxY()+1,32,32));
        var g=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB).createGraphics();
        g.setClip(0,0,32,32);assertTrue(g.hitClip(31,31,32,32));assertFalse(g.hitClip(33,33,32,32));g.dispose();
    }
}
