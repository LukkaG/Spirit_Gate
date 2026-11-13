package com.projetok.prototype;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PixelPerfectCameraTest {
    @Test void oddWindowSizesAndMapEdgesKeepIntegerPixelsAndInvertibleAim() {
        for(int[] size:new int[][]{{800,600},{1193,717},{1260,750},{2601,1603}})
            for(int zoom=1;zoom<=4;zoom++)for(double target:new double[]{0,500.75,1919}) {
                var v=ArenaViewport.pixelPerfect(size[0],size[1],1920,1280,target,target,zoom);
                assertEquals(1,v.scale());assertEquals(Math.floor(v.originX()),v.originX());
                assertEquals(Math.floor(v.originY()),v.originY());assertEquals(Math.floor(v.offsetX()),v.offsetX());
                assertEquals(Math.floor(v.offsetY()),v.offsetY());
                assertTrue(v.originX()>=0&&v.originY()>=0);
                assertTrue(v.visibleWorld().getMaxX()<=1920&&v.visibleWorld().getMaxY()<=1280);
                double x=v.originX()+1,y=v.originY()+1;
                var screen=v.worldToScreen(x,y);var restored=v.screenToWorld(screen.x,screen.y);
                assertEquals(Math.floor(screen.x),screen.x);assertEquals(Math.floor(screen.y),screen.y);
                assertTrue(v.containsScreen(screen.x,screen.y));assertEquals(x,restored.x);assertEquals(y,restored.y);
                assertFalse(v.containsScreen(v.offsetX()-1,v.offsetY()));
                assertFalse(v.containsScreen(v.offsetX()+v.arenaWidth(),v.offsetY()));
            }
    }

    @Test void smallWindowCropsMoreWorldInsteadOfShrinkingPixels() {
        var small=ArenaViewport.pixelPerfect(800,600,960,640,480,320,2);
        var full=ArenaViewport.pixelPerfect(1260,750,960,640,480,320,2);
        assertEquals(2,small.worldToScreen(201,200).x-small.worldToScreen(200,200).x);
        assertEquals(2,full.worldToScreen(201,200).x-full.worldToScreen(200,200).x);
        assertTrue(small.visibleWorld().width<full.visibleWorld().width);
        assertEquals(480,full.visibleWorld().width);
    }

    @Test void fractionalZoomClampsMapBordersAndLetterboxAndRoundTripsAim() {
        for(int[] size:new int[][]{{800,600},{1193,717},{1260,750},{2601,1603}})
            for(int[] map:new int[][]{{960,640},{1920,1280}})
                for(double zoom:new double[]{.75,1.25})for(double target:new double[]{0,500.75,1919}) {
                    var v=ArenaViewport.pixelPerfect(size[0],size[1],map[0],map[1],target,target,zoom);
                    assertEquals(zoom,v.zoom());assertEquals(1,v.scale());
                    assertTrue(v.originX()>=0&&v.originY()>=0);
                    assertTrue(v.visibleWorld().getMaxX()<=map[0]&&v.visibleWorld().getMaxY()<=map[1]);
                    double x=v.originX()+10.25,y=v.originY()+10.5;
                    var screen=v.worldToScreen(x,y);var restored=v.screenToWorld(screen.x,screen.y);
                    assertEquals(x,restored.x,1e-9);assertEquals(y,restored.y,1e-9);
                    assertEquals(zoom,v.worldToScreen(x+1,y).x-screen.x,1e-9);
                    assertTrue(v.containsScreen(screen.x,screen.y));
                    assertFalse(v.containsScreen(v.offsetX()-1,v.offsetY()));
                    assertFalse(v.containsScreen(v.offsetX()+v.arenaWidth(),v.offsetY()));
                }
        var full=ArenaViewport.pixelPerfect(1260,750,960,640,480,320,.75);
        assertEquals(0,full.originX());assertEquals(0,full.originY());
        assertEquals(960,full.visibleWorld().width);assertEquals(640,full.visibleWorld().height);
        assertEquals(120,full.offsetX());assertEquals(80,full.offsetY());
        assertFalse(full.containsScreen(100,100));
    }

    @Test void fractionalRendererKeepsPaletteAndHudAndRejectsInvalidZoom() {
        var snapshot=DebugWorld.demo(new Random(2)).snapshot();
        var source=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        var colors=new int[]{Color.RED.getRGB(),Color.GREEN.getRGB(),Color.BLUE.getRGB(),Color.YELLOW.getRGB()};
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)source.setRGB(x,y,colors[(x+y)%4]);
        var palette=Set.of(colors[0],colors[1],colors[2],colors[3]);
        for(double zoom:new double[]{.75,1.25})for(int[] size:new int[][]{{800,600},{1193,717},{1260,750}}) {
            var panel=new DebugPanel(()->snapshot);panel.setSize(size[0],size[1]);
            panel.setPixelPerfectCamera(true);panel.setCameraZoom(zoom);assertEquals(zoom,panel.getCameraZoom());
            var v=panel.viewport(snapshot);int x=(int)v.originX()+20,y=(int)v.originY()+20;
            panel.setWorldOverlay(g->{g.setColor(Color.BLACK);g.fillRect(0,0,960,640);g.drawImage(source,x,y,null);});
            var output=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            panel.render(g,snapshot);g.dispose();var p=v.worldToScreen(x,y);var found=new HashSet<Integer>();
            for(int sy=0;sy<(int)(8*zoom);sy++)for(int sx=0;sx<(int)(8*zoom);sx++) {
                int color=output.getRGB((int)p.x+sx,(int)p.y+sy);assertTrue(palette.contains(color));found.add(color);
            }
            assertEquals(palette,found);
            var aimed=panel.aimAt((float)p.x,(float)p.y);assertEquals(x,aimed.x,1e-5);assertEquals(y,aimed.y,1e-5);
            var layout=ArenaViewport.fit(size[0],size[1],960,640,0,0,1);
            int hudX=(int)(layout.offsetX()+1000*layout.scale()),hudY=(int)(layout.offsetY()+500*layout.scale());
            assertFalse(v.containsScreen(hudX,hudY));assertTrue(Double.isNaN(panel.aimAt(hudX,hudY).x));
            assertNotEquals(Color.BLACK.getRGB(),output.getRGB(hudX,hudY));
        }
        var panel=new DebugPanel(()->snapshot);panel.setPixelPerfectCamera(true);
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,0,-1,.1,4.1}) {
            assertThrows(IllegalArgumentException.class,()->panel.setCameraZoom(invalid));
            assertThrows(IllegalArgumentException.class,()->ArenaViewport.pixelPerfect(1260,750,960,640,0,0,invalid));
        }
        panel.setCameraZoom(.75);panel.setPixelPerfectCamera(false);assertEquals(1,panel.getCameraZoom());
        assertThrows(IllegalArgumentException.class,()->panel.setCameraZoom(.75));
        assertThrows(IllegalArgumentException.class,()->panel.setCameraZoom(0));
        assertThrows(IllegalArgumentException.class,()->ArenaViewport.fit(1260,750,960,640,0,0,0));
    }

    @Test void rendererCopiesPixelsInUniformTwoByTwoBlocksAndLeavesHudSeparate() {
        var snapshot=DebugWorld.demo(new Random(2)).snapshot();
        var source=new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB);
        int[][] colors={{Color.RED.getRGB(),Color.GREEN.getRGB(),Color.BLUE.getRGB()},
                        {Color.CYAN.getRGB(),Color.MAGENTA.getRGB(),Color.YELLOW.getRGB()}};
        for(int y=0;y<2;y++)for(int x=0;x<3;x++)source.setRGB(x,y,colors[y][x]);
        for(int[] size:new int[][]{{800,600},{1193,717},{1260,750}}) {
            var panel=new DebugPanel(()->snapshot);panel.setSize(size[0],size[1]);
            panel.setPixelPerfectCamera(true);panel.setCameraZoom(2);
            assertTrue(panel.isPixelPerfectCamera());assertEquals(2,panel.getCameraZoom());
            var v=panel.viewport(snapshot);int x=(int)v.originX()+20,y=(int)v.originY()+20;
            panel.setWorldOverlay(g->{g.setColor(Color.BLACK);g.fillRect(0,0,960,640);g.drawImage(source,x,y,null);});
            var output=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            panel.render(g,snapshot);g.dispose();
            var p=v.worldToScreen(x,y);
            for(int sy=0;sy<2;sy++)for(int sx=0;sx<3;sx++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
                assertEquals(colors[sy][sx],output.getRGB((int)p.x+sx*2+dx,(int)p.y+sy*2+dy));
            // O HUD continua ocupando a faixa da direita, fora do recorte da arena.
            var layout=ArenaViewport.fit(size[0],size[1],960,640,0,0,2);
            int hudX=(int)(layout.offsetX()+1000*layout.scale()),hudY=(int)(layout.offsetY()+500*layout.scale());
            assertFalse(v.containsScreen(hudX,hudY));
            assertNotEquals(Color.BLACK.getRGB(),output.getRGB(hudX,hudY));
            var aimed=panel.aimAt((float)p.x,(float)p.y);assertEquals(x,aimed.x);assertEquals(y,aimed.y);
            assertTrue(Double.isNaN(panel.aimAt(hudX,hudY).x));
        }
    }
}
