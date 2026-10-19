package com.projetok.prototype;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioLayerTest {
    @Test void foregroundCoversActorsAndNeverChangesCollision() {
        var base=LargeMapTest.large();
        var back=new LocalArenaMap(1920,1280,1500,1000,List.of(new LocalArenaMap.Visual("back",1480,980,40,40,2,0xff00ff)),base.physical());
        var front=new LocalArenaMap(1920,1280,1500,1000,List.of(new LocalArenaMap.Visual("front",1480,980,40,40,6,0xff00ff)),base.physical());
        var a=DebugWorld.forMap(back,new Random(2));var b=DebugWorld.forMap(front,new Random(2));
        assertEquals(a.snapshot(),b.snapshot());assertEquals(back.obstacles(),front.obstacles());
        int[] colors=new int[2];int index=0;
        for(var map:List.of(back,front)) {
            var panel=new DebugPanel(a::snapshot);panel.setLocalMap(map);
            var image=new BufferedImage(1260,750,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            panel.render(g,a.snapshot());g.dispose();var p=panel.viewport(a.snapshot()).worldToScreen(1505,1005);
            colors[index++]=image.getRGB((int)p.x,(int)p.y);
        }
        assertNotEquals(colors[0],colors[1]);assertEquals(0xffff00ff,colors[1]);
        for(int i=0;i<90;i++) {a.tick(DebugInput.NONE,1f/30f);b.tick(DebugInput.NONE,1f/30f);assertEquals(a.snapshot(),b.snapshot());}
    }
    @Test void stableLayerOrderRetainsInputOrderAndRejectsInvalidLayer() {
        var base=LargeMapTest.large();
        var map=new LocalArenaMap(1920,1280,1500,1000,List.of(
                new LocalArenaMap.Visual("a",10,10,32,32,5,0xff0000),
                new LocalArenaMap.Visual("b",10,10,32,32,5,0x0000ff)),base.physical());
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        map.renderer().drawLayers(g,5,10);g.dispose();assertEquals(0xff0000ff,image.getRGB(15,30));
        assertThrows(IllegalArgumentException.class,()->new LocalArenaMap(1920,1280,1500,1000,
                List.of(new LocalArenaMap.Visual("bad",0,0,32,32,11,0)),base.physical()));
    }
}
