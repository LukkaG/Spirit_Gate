package com.projetok.studio;

import com.projetok.rpg.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Projeção usa dimensões do documento e recusa região divergente. */
class StudioMiniMapTest {
    @Test void authoredProjectionUsesActualDimensions(){var d=WorldDocument.empty("mapa",1920,1280);var p=StudioMiniMap.project(960,640,200,100,d);assertEquals(100,p.x);assertEquals(50,p.y);}
    @Test void minimapPaintNeverNeedsLegacyMapOrDisk(){var d=WorldDocument.empty("mapa",960,640);var w=new RpgWorld();var map=new StudioMiniMap(()->d,w::snapshot);map.setSize(200,180);var surface=new BufferedImage(200,180,2);var g=surface.createGraphics();assertDoesNotThrow(()->map.paint(g));g.dispose();assertNotEquals(0,surface.getRGB(10,50));}
}
