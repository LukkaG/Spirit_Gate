package com.projetok.multiplayer;

import com.projetok.prototype.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task213HudTest {
    @Test void overlayLayoutKeepsControlsOnScreenAndWorldUnreserved()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var world=new DebugPanel(()->DebugWorld.demo().snapshot());world.setPixelPerfectCamera(true);world.setOverlayHud(true);
            var bars=new NetworkCombatHud();
            for(var size:new int[][]{{640,480},{1000,700},{1366,768}}){
                world.setSize(size[0],size[1]);var visible=world.visibleWorld();
                assertEquals(Math.min(960,size[0]),visible.getWidth(),.01);
                assertEquals(Math.min(640,size[1]),visible.getHeight(),.01);
            }
            bars.overlayStyle();bars.setSize(600,58);
            var effects=new NetworkCombatPresentation();bars.update(new Protocol.State(1,List.of(new Protocol.Actor(1,"Jogador",140,300,37,125,640,0,false))),1,effects);
            assertEquals("HP 37 / 125",bars.healthText());assertEquals("Energia 64 / 100",bars.energyText());
            var image=new BufferedImage(600,58,2);var g=image.createGraphics();try{bars.printAll(g);}finally{g.dispose();}
            bars.update(null,1,effects);assertTrue(bars.healthText().contains("aguardando"));
        });
    }
    @Test void immersiveCameraAndMinimapHaveConsistentTransforms() {
        var viewport=ArenaViewport.immersive(1366,768,960,640,140,300,1.25);
        assertEquals(1200,viewport.arenaWidth());assertEquals(768,viewport.arenaHeight());
        var screen=viewport.worldToScreen(140,300);var point=viewport.screenToWorld(screen.x,screen.y);
        assertEquals(140,point.x,.001);assertEquals(300,point.y,.001);
        var map=NetworkMinimap.project(480,320,180,120,960,640);assertEquals(90,map.getX());assertEquals(60,map.getY());
        assertThrows(IllegalArgumentException.class,()->NetworkMinimap.project(1,1,0,120,960,640));
        assertThrows(IllegalArgumentException.class,()->ArenaViewport.immersive(0,720,960,640,100,100,1));
    }
}
