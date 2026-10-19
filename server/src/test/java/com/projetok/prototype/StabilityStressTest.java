package com.projetok.prototype;

import com.projetok.domain.skill.SkillCastCommand;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StabilityStressTest {
    @Test void sixThousandTicksCommandsRestartsAndAnimationsAreDeterministic() throws Exception {
        var geometry=LargeMapTest.large();
        var map=new LocalArenaMap(1920,1280,135,340,List.of(),geometry.physical());
        int freezes=0,ice=0,dash=0,restarts=0;
        try(var a=new DebugRuntime(()->DebugWorld.forMap(map,new Random(24)));
            var b=new DebugRuntime(()->DebugWorld.forMap(map,new Random(24)))) {
            for(int i=0;i<6000;i++) {
                if(i%600==0) {a.restart();b.restart();restarts++;}
                var p=a.snapshot().actors().get(0);
                if(i%150==0) {
                    var mob=a.snapshot().actors().get(1);
                    var command=new SkillCastCommand("cryostasis",mob.x(),mob.y());a.cast(command);b.cast(command);
                }
                if(i%150==30) {
                    var command=new SkillCastCommand("lightning_step",p.x()+200,p.y()+50);a.cast(command);b.cast(command);
                }
                int phase=(i/30)%4;
                var input=new DebugInput(phase==0,phase==2,phase==3,phase==1,i%20<10,i%200==0);
                a.setInput(input);b.setInput(input);a.advanceOneTick();b.advanceOneTick();
                assertEquals(a.presentation(),b.presentation(),"tick="+i);
                var snapshot=a.snapshot();
                assertTrue(Float.isFinite(snapshot.stamina()));assertTrue(snapshot.stamina()>=0 && snapshot.stamina()<=snapshot.maxStamina());
                for(var actor:snapshot.actors()) {assertTrue(Float.isFinite(actor.x()+actor.y()));assertTrue(actor.x()>=actor.radius() && actor.x()<=1920-actor.radius());}
                if(!snapshot.statuses().isEmpty())freezes++;
                if(!snapshot.iceZones().isEmpty())ice++;
                if(snapshot.lightningStepActive())dash++;
            }
        }
        assertTrue(freezes>0);assertTrue(ice>0);assertTrue(dash>0);assertEquals(10,restarts);
        Files.writeString(Path.of("../docs/TASK-035-estresse.json"),
                "{\"ticksPerRuntime\":6000,\"snapshotComparisons\":6000,\"restartsPerRuntime\":"+restarts+
                ",\"ticksWithFreeze\":"+freezes+",\"ticksWithIce\":"+ice+",\"ticksWithDash\":"+dash+",\"seed\":24}\n");
    }
    @Test void maximumVisualCountRenderingAndRepeatedSolidDashStayValid() {
        var base=LargeMapTest.large();List<LocalArenaMap.Visual> visuals=new ArrayList<>();
        for(int i=0;i<4096;i++)visuals.add(new LocalArenaMap.Visual("tile-"+i,(i%128)*32,(i/128)*32,32,32,i%11,0x245342));
        var map=new LocalArenaMap(8192,8192,1500,1000,visuals,List.of(
                new LocalArenaMap.Physical("boundary",0,0,8192,8192,com.projetok.domain.movement.CollisionCategory.WORLD_BOUNDARY,null)));
        var world=DebugWorld.forMap(map,new Random(4));var panel=new DebugPanel(world::snapshot);panel.setLocalMap(map);
        var before=world.snapshot();var image=new BufferedImage(1260,750,2);var g=image.createGraphics();
        for(int zoom=1;zoom<=4;zoom++){panel.setCameraZoom(zoom);panel.render(g,before);}g.dispose();assertEquals(before,world.snapshot());
        for(int i=0;i<32;i++) {
            var w=DebugWorld.forMap(base,new Random(i));
            w.tickQueued(DebugInput.NONE,1f/30f,0,List.of(new SkillCastCommand("lightning_step",1800,1000)));
            for(int j=0;j<20;j++)w.tick(DebugInput.NONE,1f/30f);
            var player=w.snapshot().actors().get(0);assertTrue(player.x()<=1650-player.radius());
        }
        for(double target:new double[]{-1e9,0,8192,1e9})for(int zoom=1;zoom<=4;zoom++) {
            var viewport=ArenaViewport.fit(800,600,8192,8192,target,target,zoom);
            assertTrue(viewport.originX()>=0 && viewport.visibleWorld().getMaxX()<=8192);
        }
    }
}
