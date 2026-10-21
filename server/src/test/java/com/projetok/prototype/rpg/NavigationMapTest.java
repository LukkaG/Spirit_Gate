package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NavigationMapTest {
    @Test void portalTransitionRefreshesTheMapWithoutChangingTravelRules(){
        var source=new RpgWorld();var save=source.save();var portal=source.snapshot().portals().get(0);
        var world=RpgWorld.restore(new RpgSave(save.identity(),save.region(),portal.x(),portal.y(),save.hp(),save.xp(),save.coins(),save.inventory(),save.equipment(),save.quests(),save.flags(),save.ticks()));
        try(var runtime=new RpgRuntime(world)){
            var map=new NavigationMap(runtime);map.setSize(300,240);
            var image=new java.awt.image.BufferedImage(300,240,2);var g=image.createGraphics();map.paint(g);
            int[] before=image.getRGB(0,0,300,240,null,0,300);
            runtime.command(RpgWorld::interact);runtime.advanceOneTick();assertEquals(portal.destination(),runtime.snapshot().state().region());
            map.paint(g);g.dispose();assertFalse(java.util.Arrays.equals(before,image.getRGB(0,0,300,240,null,0,300)));
        }
    }
    @Test void projectionZoomAndPaintingPreserveCollisionAndSave(){
        var world=new RpgWorld();var before=world.save();try(var runtime=new RpgRuntime(world)){
            var map=new NavigationMap(runtime);var point=NavigationMap.project(480,320,200,100,RpgMaps.region("village"));assertEquals(100,point.getX());assertEquals(50,point.getY());
            assertThrows(IllegalArgumentException.class,()->map.zoom(Double.NaN));assertThrows(IllegalArgumentException.class,()->map.zoom(0));
            map.setSize(200,160);var image=new java.awt.image.BufferedImage(200,160,2);var g=image.createGraphics();for(double zoom:new double[]{1,2,3}){map.zoom(zoom);map.paint(g);}g.dispose();assertEquals(before,world.save());
        }
    }
}
