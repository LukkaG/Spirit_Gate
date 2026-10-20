package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
class StudioViewportTest {
    @Test void zoomAnchorsCursorAndPanRoundTrips(){var v=new StudioViewport(.5,40,-20);var p=v.world(200,100);var next=v.zoomAt(2,200,100);assertEquals(p,next.world(200,100));assertEquals(200,next.screen(p.x,p.y).x);assertThrows(IllegalArgumentException.class,()->new StudioViewport(Double.NaN,0,0));}
    @Test void canvasPaintsFromMemoryWithoutFiles(){var cache=new AssetImages();var panel=new WorldCanvas(()->WorldDocument.empty("mapa",960,640),cache);panel.setSize(900,600);var image=new BufferedImage(900,600,2);var g=image.createGraphics();panel.paint(g);g.dispose();assertEquals(0,cache.bytes());assertNotEquals(0,image.getRGB(100,100));}
}
