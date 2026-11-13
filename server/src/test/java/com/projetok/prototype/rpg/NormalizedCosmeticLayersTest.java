package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.multiplayer.NetworkPlayerVisuals;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NormalizedCosmeticLayersTest {
    private final Path folder=Path.of("../assets/dev/generated/corujao-iv-v1");
    @Test void allAuthoredLayersKeepTheirPivotAndExpandTheSameLogicalPixels() throws Exception {
        var source=CosmeticCatalog.load(folder);assertTrue(source.warnings().isEmpty());
        var normalized=source.onLogicalCanvas(64,64,32,60,.4);
        var metadata=new Properties();try(var input=Files.newInputStream(folder.resolve("manifest.properties"))){metadata.load(input);}
        int count=Integer.parseInt(metadata.getProperty("count"));
        assertEquals(source.all("hair"),normalized.all("hair"));assertEquals(source.all("clothing"),normalized.all("clothing"));
        assertEquals(source.warnings(),normalized.warnings());
        for(int frame=0;frame<count;frame++){
            var fields=metadata.getProperty("frame."+frame).split("\\|");
            var logical=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=logical.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(normalized.draw(g,fields[0],fields[1],fields[2],32,60,1));}finally{g.dispose();}
            var reference=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);g=reference.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(source.draw(g,fields[0],fields[1],fields[2],32,60,.4));}finally{g.dispose();}
            var enlarged=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);g=enlarged.createGraphics();
            try{g.setComposite(AlphaComposite.Src);g.scale(2,2);assertTrue(normalized.draw(g,fields[0],fields[1],fields[2],32,60,1));}finally{g.dispose();}
            for(int y=0;y<64;y++)for(int x=0;x<64;x++){
                int expected=logical.getRGB(x,y);assertEquals(reference.getRGB(x,y),expected);
                for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)assertEquals(expected,enlarged.getRGB(x*2+dx,y*2+dy));
            }
        }
        assertThrows(IllegalArgumentException.class,()->source.onLogicalCanvas(16,16,8,16,1));
    }
    @Test void bodyAndAllCosmeticCombinationsShareTheGridAtCameraZoomTwo() throws Exception {
        var original=CosmeticCatalog.load(folder);var catalog=original.onLogicalCanvas(64,64,32,60,.4);
        var body=new NetworkPlayerVisuals(Path.of("../assets/dev/reaper"),RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5)).sprites("human");
        for(var hair:catalog.all("hair"))for(var clothing:catalog.all("clothing"))for(var direction:Direction.values())for(int frame=0;frame<=4;frame++){
            var selected=new CosmeticCatalog.Selection(hair.id(),clothing.id());catalog.validate(selected);
            var logical=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=logical.createGraphics();
            try{assertTrue(CosmeticRenderer.draw(g,body,catalog,selected,direction,Math.max(0,frame-1),frame>0,32,60));}finally{g.dispose();}
            var enlarged=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);g=enlarged.createGraphics();
            try{g.scale(2,2);assertTrue(CosmeticRenderer.draw(g,body,catalog,selected,direction,Math.max(0,frame-1),frame>0,32,60));}finally{g.dispose();}
            for(int y=0;y<64;y++)for(int x=0;x<64;x++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)
                assertEquals(logical.getRGB(x,y),enlarged.getRGB(x*2+dx,y*2+dy));
        }
    }
}
