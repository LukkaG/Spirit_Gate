package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class ArenaViewportTest {
    @Test void defaultIdentityAndResizeLetterbox() {
        var v=ArenaViewport.fit(1260,750,960,640,135,148,1);
        assertEquals(0,v.originX());assertEquals(0,v.originY());assertEquals(1,v.scale());
        assertEquals(135,v.worldToScreen(135,148).x);assertEquals(148,v.screenToWorld(135,148).y);
        var large=ArenaViewport.fit(2600,1600,960,640,135,148,1);
        assertEquals(2,large.scale());assertTrue(large.offsetX()>0);
        assertFalse(large.containsScreen(0,0));
        assertFalse(v.containsScreen(1000,100)); // HUD nao vira alvo.
        assertFalse(v.containsScreen(Double.NaN,Double.NaN));
    }
    @Test void largeSyntheticWorldClampedRoundTripsAllSizesAndZooms() {
        for(int zoom=1;zoom<=4;zoom++) for(int width:new int[]{630,1260,2600})
            for(double target:new double[]{0,500,2000}) {
                var v=ArenaViewport.fit(width,width*750/1260,1920,1280,target,target,zoom);
                assertTrue(v.originX()>=0 && v.originX()+960d/zoom<=1920);
                assertTrue(v.originY()>=0 && v.originY()+640d/zoom<=1280);
                for(double[] p:new double[][]{{0,0},{1919,1279},{783.25,432.75}}) {
                    var screen=v.worldToScreen(p[0],p[1]);var world=v.screenToWorld(screen.x,screen.y);
                    assertEquals(p[0],world.x,1e-9);assertEquals(p[1],world.y,1e-9);
                }
            }
        assertThrows(IllegalArgumentException.class,()->ArenaViewport.fit(0,750,960,640,0,0,1));
        assertThrows(IllegalArgumentException.class,()->ArenaViewport.fit(1260,750,960,640,0,0,0));
    }
    private void mouse(DebugPanel panel,double x,double y) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var e=new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,(int)x,(int)y,0,false);
            for(var listener:panel.getMouseMotionListeners()) listener.mouseMoved(e);
        });
    }
    private void action(DebugPanel panel,String key) throws Exception {
        SwingUtilities.invokeAndWait(()->panel.getActionMap().get(key).actionPerformed(new ActionEvent(panel,0,key)));
    }
    @Test void qAndShiftUseWorldAfterZoomResizeAndRestart() throws Exception {
        for(String key:new String[]{"Q","SHIFT"}) try(var actual=new DebugRuntime(()->DebugWorld.demo(new Random(5)));
                                                    var reference=new DebugRuntime(()->DebugWorld.demo(new Random(5)))) {
            var panel=new DebugPanel(actual::snapshot);panel.setCameraZoom(4);panel.setSize(2520,1500);
            SwingUtilities.invokeAndWait(()->DebugLauncher.configureKeys(panel,actual,new HashSet<>()));
            var v=panel.viewport(actual.snapshot());assertTrue(v.originX()>0 && v.originY()>0);
            float targetX=actual.snapshot().actors().get(0).x()+80,targetY=actual.snapshot().actors().get(0).y();
            var screen=v.worldToScreen(targetX,targetY);assertTrue(v.containsScreen(screen.x,screen.y));mouse(panel,screen.x,screen.y);
            action(panel,"press_"+key);
            reference.cast(new com.projetok.domain.skill.SkillCastCommand(key.equals("Q")?"cryostasis":"lightning_step",targetX,targetY));
            actual.advanceOneTick();reference.advanceOneTick();
            assertEquals(reference.snapshot(),actual.snapshot());
            action(panel,"restart");actual.advanceOneTick();reference.restart();reference.advanceOneTick();
            assertEquals(reference.snapshot(),actual.snapshot());
        }
    }
    @Test void renderingRepeatedIsPureAndHudDoesNotFollowCamera() throws Exception {
        var world=DebugWorld.demo(new Random(2));var snapshot=world.snapshot();var panel=new DebugPanel(()->snapshot);
        var normal=new BufferedImage(1260,750,2);var g=normal.createGraphics();panel.render(g,snapshot);g.dispose();
        panel.setCameraZoom(4);panel.setShowHitboxes(true);
        var zoomed=new BufferedImage(1260,750,2);g=zoomed.createGraphics();panel.render(g,snapshot);panel.render(g,snapshot);g.dispose();
        assertEquals(snapshot,world.snapshot());
        // HUD pixels remain identical despite camera and world zoom.
        for(int y=0;y<640;y+=17)for(int x=980;x<1260;x+=17)assertEquals(normal.getRGB(x,y),zoomed.getRGB(x,y));
        g=zoomed.createGraphics();g.setColor(java.awt.Color.MAGENTA);g.drawString("CENARIO SINTETICO - CAMERA 4X / HITBOXES",20,730);g.dispose();
        ImageIO.write(zoomed,"png",Path.of("../docs/TASK-020-camera-sintetica.png").toFile());
    }
}
