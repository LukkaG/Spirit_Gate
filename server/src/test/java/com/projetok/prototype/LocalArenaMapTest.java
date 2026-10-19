package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.domain.skill.LightningStepService;
import static org.junit.jupiter.api.Assertions.*;

class LocalArenaMapTest {
    @TempDir Path temp;
    private Path source=Path.of("../docs/mapa-demo.properties");
    private LocalArenaMap load() throws Exception {return LocalArenaMap.load(source);}
    @Test void loadsRepeatablyAndPreservesDefaultDemo() throws Exception {
        var map=load();assertEquals(map,load());
        assertThrows(UnsupportedOperationException.class,()->map.visuals().clear());
        assertEquals(DebugWorld.demo(new Random(3)).snapshot(),DebugWorld.forMap(map,new Random(3)).snapshot());
        assertEquals(3,DebugWorld.demo().snapshot().softObstacles().size());
    }
    @Test void rejectsInvalidBoundsSpawnsCountsSchemaAndObjects() throws Exception {
        String original=Files.readString(source);
        String[][] changes={{"width=960","width=NaN"},{"spawnX=135","spawnX=360"},
                {"spawnY=340","spawnY=0"},{"physical.count=7","physical.count=999999"},
                {"schemaVersion=1","schemaVersion=2"},{"physical.4.x=70","physical.4.x=-1"},
                {"physical.4.id=arbusto-a","physical.4.id=predio-a"},
                {"physical.4.x=70","physical.4.x=160"},{"physical.4.category=SOFT_OBSTACLE","physical.4.category=ACTOR"},
                {"physical.0.width=960","physical.0.width=900"}};
        for(var pair:changes) {
            Path bad=temp.resolve("bad.properties");Files.writeString(bad,original.replace(pair[0],pair[1]));
            assertThrows(Exception.class,()->LocalArenaMap.load(bad),pair[0]);
        }
        Path bad=temp.resolve("extra.properties");Files.writeString(bad,original+"unknown=1\n");
        assertThrows(Exception.class,()->LocalArenaMap.load(bad));
        Files.writeString(bad,original+"width=960\n");assertThrows(Exception.class,()->LocalArenaMap.load(bad));
    }
    @Test void visualDimensionsNeverChangeCollisionOrReplay() throws Exception {
        var map=load();var large=List.of(new LocalArenaMap.Visual("gigante",0,0,960,640,0,0x332244));
        var other=new LocalArenaMap(map.width(),map.height(),map.spawnX(),map.spawnY(),large,map.physical());
        assertEquals(map.obstacles(),other.obstacles());
        var a=DebugWorld.forMap(map,new Random(3));var b=DebugWorld.forMap(other,new Random(3));
        for(int i=0;i<180;i++) {
            var input=new DebugInput(i%60<30,i%60>=30,false,false,i%3==0);
            a.tick(input,1f/30f);b.tick(input,1f/30f);assertEquals(a.snapshot(),b.snapshot());
        }
    }
    @Test void mapMovementAndDashMatchReferenceEveryTick() throws Exception {
        var a=DebugWorld.forMap(load(),new Random(4));var b=DebugWorld.demo(new Random(4));
        for(int i=0;i<20;i++) {
            var input=new DebugInput(false,true,true,false,false);
            a.tick(input,1f/30f);b.tick(input,1f/30f);assertEquals(a.snapshot(),b.snapshot());
        }
        var cast=new SkillCastCommand(LightningStepService.ID,80,600);
        a.tickQueued(DebugInput.NONE,1f/30f,0,List.of(cast));b.tickQueued(DebugInput.NONE,1f/30f,0,List.of(cast));
        for(int i=0;i<20;i++) {a.tick(DebugInput.NONE,1f/30f);b.tick(DebugInput.NONE,1f/30f);assertEquals(a.snapshot(),b.snapshot());}
    }
    @Test void restartUsesSameLocalMap() throws Exception {
        var map=load();
        try(var a=new DebugRuntime(()->DebugWorld.forMap(map,new Random(6)));var b=new DebugRuntime(()->DebugWorld.forMap(map,new Random(6)))) {
            a.setInput(new DebugInput(false,true,false,false,false));for(int i=0;i<20;i++)a.advanceOneTick();
            a.setInput(DebugInput.NONE);a.restart();a.advanceOneTick();b.advanceOneTick();
            assertEquals(b.snapshot(),a.snapshot());assertEquals(b.presentation(),a.presentation());
        }
    }
    @Test void syntheticRenderHasFallbackAndDoesNotMutate() throws Exception {
        var map=load();var world=DebugWorld.forMap(map,new Random(1));var snapshot=world.snapshot();
        var panel=new DebugPanel(()->snapshot);panel.setLocalMap(map);
        var image=new BufferedImage(DebugPanel.PANEL_WIDTH,DebugPanel.PANEL_HEIGHT,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();panel.render(g,snapshot);panel.render(g,snapshot);g.dispose();
        assertEquals(snapshot,world.snapshot());assertEquals(map,load());
        ImageIO.write(image,"png",Path.of("../docs/TASK-015-placeholders.png").toFile());
    }
}
