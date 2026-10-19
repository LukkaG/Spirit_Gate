package com.projetok.prototype;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.domain.skill.LightningStepService;
import com.projetok.domain.movement.CollisionCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LargeMapTest {
    @TempDir Path temp;
    static LocalArenaMap large() {
        return new LocalArenaMap(1920,1280,1500,1000,List.of(),List.of(
                new LocalArenaMap.Physical("boundary",0,0,1920,1280,CollisionCategory.WORLD_BOUNDARY,null),
                new LocalArenaMap.Physical("solid",1650,900,32,200,CollisionCategory.SOLID_OBSTACLE,null),
                new LocalArenaMap.Physical("soft",1450,900,32,32,CollisionCategory.SOFT_OBSTACLE,DebugSnapshot.SoftObstacleKind.BUSH)));
    }
    @Test void v2LoadsAndV1RemainsStrict() throws Exception {
        String original=Files.readString(Path.of("../docs/mapa-demo.properties"));
        String v2=original.replace("schemaVersion=1","schemaVersion=2").replace("width=960","width=1920").replace("height=640","height=1280");
        Path path=temp.resolve("large.properties");Files.writeString(path,v2);
        assertEquals(1920,LocalArenaMap.load(path).width());
        Files.writeString(path,v2.replace("schemaVersion=2","schemaVersion=1"));
        assertThrows(java.io.IOException.class,()->LocalArenaMap.load(path));
        Files.writeString(path,v2.replace("width=1920","width=999999"));
        assertThrows(java.io.IOException.class,()->LocalArenaMap.load(path));
    }
    @Test void movementDashCollisionCameraAndRestartUseExpandedWorld() {
        var map=large();var world=DebugWorld.forMap(map,new Random(2));
        assertEquals(1920,world.worldWidth());assertEquals(1280,world.worldHeight());
        world.tickQueued(DebugInput.NONE,1f/30f,0,List.of(new SkillCastCommand(LightningStepService.ID,1800,1000)));
        for(int i=0;i<20;i++)world.tick(DebugInput.NONE,1f/30f);
        var player=world.snapshot().actors().get(0);
        assertTrue(player.x()>1500 && player.x()<=1650-player.radius());
        var panel=new DebugPanel(world::snapshot);panel.setLocalMap(map);
        var view=panel.viewport(world.snapshot());assertTrue(view.originX()>0);assertTrue(view.originY()>0);
        var screen=view.worldToScreen(player.x(),player.y());
        assertEquals(player.x(),view.screenToWorld(screen.x,screen.y).x,1e-6);
        try(var a=new DebugRuntime(()->DebugWorld.forMap(map,new Random(2)));var b=new DebugRuntime(()->DebugWorld.forMap(map,new Random(2)))) {
            a.setInput(new DebugInput(false,true,false,false,false));
            for(int i=0;i<60;i++)a.advanceOneTick();
            a.setInput(DebugInput.NONE);a.restart();a.advanceOneTick();b.advanceOneTick();
            assertEquals(b.presentation(),a.presentation());
        }
    }
    @Test void rejectsNonFiniteSpawnsAndExcessiveObjects() {
        var map=large();
        assertThrows(IllegalArgumentException.class,()->new LocalArenaMap(1920,1280,Float.NaN,1000,List.of(),map.physical()));
        assertThrows(IllegalArgumentException.class,()->new LocalArenaMap(1920,1280,1500,1000,
                java.util.Collections.nCopies(4097,new LocalArenaMap.Visual("v",0,0,32,32,0,0)),map.physical()));
        assertThrows(IllegalArgumentException.class,()->new LocalArenaMap(8193,1280,1500,1000,List.of(),map.physical()));
    }
}
