package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class EnvironmentPreviewTest {
    @Test void modesAreVisualOnlyAndOffIsTransparent(){var d=WorldDocument.empty("mapa",128,128);for(var mode:EnvironmentPreview.Mode.values()){var b=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);var g=b.createGraphics();EnvironmentPreview.draw(g,new Rectangle(0,0,100,100),mode,1_000_000_000);g.dispose();if(mode==EnvironmentPreview.Mode.OFF)assertEquals(0,b.getRGB(20,20));assertEquals(d.hitboxes(),d.withMetadata(Map.of("preview.environment",mode.name())).hitboxes());}assertEquals(EnvironmentPreview.Mode.OFF,EnvironmentPreview.mode(d.withMetadata(Map.of("preview.environment","unknown"))));}
}
