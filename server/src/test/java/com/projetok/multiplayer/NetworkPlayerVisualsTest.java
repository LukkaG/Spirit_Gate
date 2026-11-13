package com.projetok.multiplayer;

import com.projetok.prototype.rpg.RpgPlayerSprites;
import java.nio.file.Path;
import java.util.List;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Properties;
import javax.imageio.ImageIO;
import com.projetok.prototype.DirectionalAnimation.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkPlayerVisualsTest {
    @Test void selectsVerifiedReaperWithoutReplacingHumanBody() throws Exception {
        Path assets=Path.of("../assets/dev");
        var legacy=RpgPlayerSprites.load(assets.resolve("player-base"),250,.5);
        var visuals=new NetworkPlayerVisuals(assets.resolve("reaper"),legacy);
        assertEquals(1,visuals.sprites("human").scale());
        assertSame(visuals.sprites("human"),visuals.sprites(""));
        assertNotSame(legacy,visuals.sprites("reaper"));
        assertEquals(8,visuals.sprites("reaper").walkFrames());
        assertEquals(1.0,visuals.sprites("reaper").scale());
        assertEquals(125,visuals.frameMillis("reaper"));
    }
    @Test void reaperAtOneAndHalfScaleKeepsFootPivotAndSourcePalette() throws Exception {
        Path folder=Path.of("../assets/dev/reaper");
        var sprites=RpgPlayerSprites.load(folder,125,1).withScale(1.5);
        var nativeSprites=RpgPlayerSprites.load(folder,125,1);
        var metadata=new Properties();
        try(var input=RpgPlayerSprites.class.getResourceAsStream("/rpg/reaper-player.properties")){metadata.load(input);}
        for(var direction:Direction.values())for(int frame=0;frame<=8;frame++){
            var source=ImageIO.read(folder.resolve(metadata.getProperty(direction+"."+frame).split(",")[0]).toFile());
            var palette=new HashSet<Integer>();palette.add(0);
            for(int y=0;y<64;y++)for(int x=0;x<64;x++)palette.add(source.getRGB(x,y));
            var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
            var g=image.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(sprites.drawFrame(g,direction,Math.max(0,frame-1),frame>0,128,160));}finally{g.dispose();}
            int colored=0;
            // O pivo (32,60) em 1.5x coloca o canvas 96x96 em (80,70).
            for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                int pixel=image.getRGB(x,y);
                assertTrue(palette.contains(pixel),"Nearest-neighbor nao pode criar cores intermediarias");
                if((pixel>>>24)!=0){colored++;assertTrue(x>=80&&x<176&&y>=70&&y<166);}
            }
            assertTrue(colored>0);
            var nativeBounds=nativeSprites.visualBounds(direction,Math.max(0,frame-1),frame>0,32,60);
            var bounds=sprites.visualBounds(direction,Math.max(0,frame-1),frame>0,128,160);
            assertEquals(nativeBounds.getWidth()*1.5,bounds.getWidth());
            assertEquals(nativeBounds.getHeight()*1.5,bounds.getHeight());
            assertTrue(bounds.getMaxY()<=160);
        }
    }
    @Test void nativeReaperAndHumanHaveComparableVisibleHeightAcrossAllPoses() throws Exception {
        Path assets=Path.of("../assets/dev");
        var visuals=new NetworkPlayerVisuals(assets.resolve("reaper"),RpgPlayerSprites.load(assets.resolve("player-base"),250,.5));
        var reaper=visuals.sprites("reaper");var human=visuals.sprites("human");
        var metadata=new Properties();
        try(var input=RpgPlayerSprites.class.getResourceAsStream("/rpg/reaper-player.properties")){metadata.load(input);}
        for(var direction:Direction.values())for(int frame=0;frame<=8;frame++){
            int walk=Math.max(0,frame-1);boolean walking=frame>0;
            var reaperBounds=reaper.visualBounds(direction,walk,walking,128,160);
            var humanBounds=human.visualBounds(direction,walk%human.walkFrames(),walking,128,160);
            assertTrue(Math.abs(reaperBounds.getHeight()-humanBounds.getHeight())<=6,"Altura humana deve acompanhar a arte Reaper nativa");
            var source=ImageIO.read(assets.resolve("reaper").resolve(metadata.getProperty(direction+"."+frame).split(",")[0]).toFile());
            var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(reaper.drawFrame(g,direction,walk,walking,128,160));}finally{g.dispose();}
            for(int y=0;y<64;y++)for(int x=0;x<64;x++)assertEquals(source.getRGB(x,y),image.getRGB(96+x,100+y));
        }
    }
    @Test void eightFrameWalkReachesLastFrameAndLoops() {
        var animation=new RemoteSpriteAnimation();
        for(int i=0;i<12;i++){
            animation.update(new Protocol.State(i,List.of(new Protocol.Actor(1,"Reaper",100+i,100,100,100,1000,0,false,1,0,0))),i*100);
            if(i==10){assertTrue(animation.pose(1,1000,125,8).walking());assertEquals(7,animation.pose(1,1000,125,8).frame());}
        }
        assertEquals(0,animation.pose(1,1100,125,8).frame());
        assertFalse(animation.pose(1,1700,125,8).walking());
    }
    @Test void normalizedHumanPixelsExpandUniformlyAtEveryIntegerZoom() throws Exception {
        Path assets=Path.of("../assets/dev");
        var source=RpgPlayerSprites.load(assets.resolve("player-base"),250,1);
        var sprites=new NetworkPlayerVisuals(assets.resolve("reaper"),source).sprites("human");
        for(var direction:Direction.values())for(int frame=0;frame<=4;frame++){
            int walk=Math.max(0,frame-1);boolean walking=frame>0;
            var logical=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=logical.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(sprites.drawFrame(g,direction,walk,walking,32,60));}finally{g.dispose();}
            // A grade reduzida corresponde ao desenho original de 0.4x em zoom 1.
            var old=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);g=old.createGraphics();
            try{g.setComposite(AlphaComposite.Src);assertTrue(source.withScale(.4).drawFrame(g,direction,walk,walking,32,60));}finally{g.dispose();}
            for(int y=0;y<64;y++)for(int x=0;x<64;x++)assertEquals(old.getRGB(x,y),logical.getRGB(x,y));
            for(int zoom=2;zoom<=3;zoom++){
                var enlarged=new BufferedImage(64*zoom,64*zoom,BufferedImage.TYPE_INT_ARGB);g=enlarged.createGraphics();
                try{g.setComposite(AlphaComposite.Src);g.scale(zoom,zoom);assertTrue(sprites.drawFrame(g,direction,walk,walking,32,60));}finally{g.dispose();}
                for(int y=0;y<64;y++)for(int x=0;x<64;x++)for(int dy=0;dy<zoom;dy++)for(int dx=0;dx<zoom;dx++)
                    assertEquals(logical.getRGB(x,y),enlarged.getRGB(x*zoom+dx,y*zoom+dy));
            }
        }
    }
    @Test void logicalCanvasRefusesToCropOccupiedPixels() throws Exception {
        var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,1);
        assertThrows(IllegalArgumentException.class,()->sprites.onLogicalCanvas(64,64,32,60));
        assertThrows(IllegalArgumentException.class,()->sprites.onLogicalCanvas(0,64,32,60));
    }
}
