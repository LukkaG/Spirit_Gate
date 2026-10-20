package com.projetok.prototype.rpg;

import com.projetok.prototype.*;
import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.rpg.RpgWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RpgPlayerSpritesTest {
    @TempDir Path temp;
    private Path assets() { return Path.of("../assets/dev/player-base"); }
    private Path copy() throws Exception {
        try(var files=Files.list(assets())) { for(Path file:files.toList()) Files.copy(file,temp.resolve(file.getFileName())); }
        return temp;
    }
    @Test void allDirectionsAlternateAndStopAtTheirOwnIdle() throws Exception {
        var sprites=RpgPlayerSprites.load(assets(),150);
        for(var direction:Direction.values()) {
            var world=new RpgWorld();sprites.update(world.snapshot());
            int x=direction==Direction.LEFT?-1:direction==Direction.RIGHT?1:0;
            int y=direction==Direction.BACK?-1:direction==Direction.FRONT?1:0;
            world.tick(x,y);sprites.update(world.snapshot());
            assertEquals(direction+".1",sprites.frameKey());
            for(int i=0;i<5;i++) { world.tick(x,y);sprites.update(world.snapshot()); }
            assertEquals(direction+".2",sprites.frameKey());
            for(int i=0;i<4;i++) { world.tick(x,y);sprites.update(world.snapshot()); }
            assertEquals(direction+".3",sprites.frameKey());
            world.tick(0,0);sprites.update(world.snapshot());assertEquals(direction+".0",sprites.frameKey());
        }
    }
    @Test void transitionsResetFramesAndRepeatedPaintsDoNotAdvanceTime() {
        var animation=new RpgPlayerAnimation(150);var world=new RpgWorld();
        assertEquals(new RpgPlayerAnimation.Pose(Direction.FRONT,false,0),animation.update(world.snapshot()));
        for(int i=0;i<7;i++){world.tick(1,0);animation.update(world.snapshot());}
        assertEquals(1,animation.pose().frame());
        var snapshot=world.snapshot();for(int i=0;i<100;i++)assertEquals(1,animation.update(snapshot).frame());
        world.tick(0,-1);assertEquals(new RpgPlayerAnimation.Pose(Direction.BACK,true,0),animation.update(world.snapshot()));
        world.tick(0,0);assertFalse(animation.update(world.snapshot()).walking());
        world.tick(0,-1);assertEquals(0,animation.update(world.snapshot()).frame());
    }
    @Test void configurableDurationDoesNotChangeOfficialAtlasTiming() {
        var animation=new RpgPlayerAnimation(300);var world=new RpgWorld();animation.update(world.snapshot());
        for(int i=0;i<6;i++){world.tick(1,0);animation.update(world.snapshot());}
        assertEquals(0,animation.pose().frame());
        for(int i=0;i<4;i++){world.tick(1,0);animation.update(world.snapshot());}
        assertEquals(1,animation.pose().frame());
        assertEquals(.4,DirectionalAnimation.duration(DirectionalAnimation.State.WALK));
        assertThrows(IllegalArgumentException.class,()->new RpgPlayerAnimation(0));
    }
    @Test void onlyActualMovementAnimatesIncludingBlockedMovement() {
        var animation=new RpgPlayerAnimation(150);var world=new RpgWorld();animation.update(world.snapshot());
        for(int i=0;i<50;i++){world.tick(-1,0);animation.update(world.snapshot());}
        float blockedX=world.snapshot().x();world.tick(-1,0);animation.update(world.snapshot());
        assertEquals(blockedX,world.snapshot().x());assertTrue(blockedX>=18&&blockedX<19);
        assertFalse(animation.pose().walking());
        assertEquals(Direction.LEFT,animation.pose().direction());
    }
    @Test void absentCorruptAndReplacedImagesFallBackAsAWhole() throws Exception {
        assertNull(RpgPlayerSprites.loadOptional(temp.resolve("missing"),150));
        Path folder=copy();Path file=folder.resolve("Walk_character_Front1.png");byte[] good=Files.readAllBytes(file);
        Files.delete(file);assertNull(RpgPlayerSprites.loadOptional(folder,150));
        Files.write(file,new byte[]{1,2,3});assertNull(RpgPlayerSprites.loadOptional(folder,150));
        Files.write(file,Files.readAllBytes(folder.resolve("Walk_character_Front2.png")));assertNull(RpgPlayerSprites.loadOptional(folder,150));
        Files.write(file,good);assertNotNull(RpgPlayerSprites.loadOptional(folder,150));
    }
    @Test void exactCaseIsEnforcedOnWindowsToo() throws Exception {
        Path folder=copy();Path file=folder.resolve("Main_Character_Idle_Back.png");byte[] data=Files.readAllBytes(file);
        Files.delete(file);Files.write(folder.resolve("main_character_idle_back.png"),data);
        assertNull(RpgPlayerSprites.loadOptional(folder,150));
    }
    @Test void allTwelveFramesKeepFeetAtLogicalAnchorAndAlpha() throws Exception {
        var sprites=RpgPlayerSprites.load(assets(),150);
        for(var direction:Direction.values()) {
            var world=new RpgWorld();sprites.update(world.snapshot());
            int x=direction==Direction.LEFT?-1:direction==Direction.RIGHT?1:0;
            int y=direction==Direction.BACK?-1:direction==Direction.FRONT?1:0;
            for(int state=0;state<3;state++) {
                world.tick(x,y);sprites.update(world.snapshot());
                if(state==1)for(int i=0;i<5;i++){world.tick(x,y);sprites.update(world.snapshot());}
                if(state==2){world.tick(0,0);sprites.update(world.snapshot());}
                var image=new BufferedImage(150,150,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
                assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(1,"Test",75,120,18,10,10,true,true)));g.dispose();
                int maxY=-1,minX=150,maxX=-1;
                for(int py=0;py<150;py++)for(int px=0;px<150;px++)if((image.getRGB(px,py)>>>24)!=0){maxY=Math.max(maxY,py);minX=Math.min(minX,px);maxX=Math.max(maxX,px);}
                assertTrue(maxY>=118&&maxY<=120,sprites.frameKey()+" feet="+maxY);
                assertTrue(Math.abs((minX+maxX)/2.0-75)<=1,sprites.frameKey());
                assertEquals(0,image.getRGB(0,0));assertEquals(0,image.getRGB(75,125));
            }
        }
    }
    @Test void drawingIsCachedAndDoesNotMutateWorldOrOtherActors() throws Exception {
        Path folder=copy();var sprites=RpgPlayerSprites.load(folder,150);var world=new RpgWorld();var before=world.save();sprites.update(world.snapshot());
        try(var files=Files.list(folder)){for(Path p:files.toList())Files.delete(p);}
        var image=new BufferedImage(200,200,2);var g=image.createGraphics();
        assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(1,"Player",100,150,18,10,10,true,true)));
        assertFalse(sprites.draw(g,new DebugSnapshot.ActorView(100,"Enemy",100,150,19,10,10,false,true)));
        assertFalse(sprites.draw(g,new DebugSnapshot.ActorView(1,"Dead",100,150,18,0,10,true,false)));
        g.dispose();assertEquals(before,world.save());
    }
    @Test void disabledConfigurationDoesNotTryToLoadAssets() {
        String old=System.getProperty(RpgPlayerSprites.ENABLED);
        try{System.clearProperty(RpgPlayerSprites.ENABLED);assertNull(RpgPlayerSprites.configured());}
        finally{if(old!=null)System.setProperty(RpgPlayerSprites.ENABLED,old);}
    }
    @Test void rendererWithoutHookAndWithDecliningHookArePixelIdentical() {
        var world=DebugWorld.demo().snapshot();var panel=new DebugPanel(()->world);
        var a=new BufferedImage(1260,750,2);var b=new BufferedImage(1260,750,2);
        var ga=a.createGraphics();panel.render(ga,world);ga.dispose();panel.setActorRenderer((g,actor)->false);
        var gb=b.createGraphics();panel.render(gb,world);gb.dispose();
        assertArrayEquals(a.getRGB(0,0,1260,750,null,0,1260),b.getRGB(0,0,1260,750,null,0,1260));
    }
}
