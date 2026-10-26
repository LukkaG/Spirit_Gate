package com.projetok.multiplayer;
import com.projetok.prototype.rpg.*;
import com.projetok.prototype.DirectionalAnimation.Direction;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task177AppearanceTest {
    @Test void clothingAndHairCannotAlterPositionsContactOrAttributes()throws Exception{
        var catalog=CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));
        var a=new RpgSessionWorld(List.of("reaper","human"),catalog);var b=new RpgSessionWorld(List.of("reaper","human"),catalog);
        for(var w:List.of(a,b)){w.join(1,"Player A");w.join(2,"Player B");}
        assertTrue(a.appearance(1,new Protocol.AppearanceRequest(1,"hair_spiky","light_armor")));
        assertTrue(a.appearance(2,new Protocol.AppearanceRequest(1,"hair_short","jacket")));
        for(int i=2;i<70;i++){for(var w:List.of(a,b)){w.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,0,-1));w.step();}assertEquals(b.snapshot(),a.snapshot());}
        var p=a.snapshot().actors();assertEquals(24,Math.hypot(p.get(0).x()-p.get(1).x(),p.get(0).y()-p.get(1).y()),.001);
    }
    @Test void allRealWalkFramesDrawAroundPivotWithoutChangingPhysics()throws Exception{
        var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5);var w=new RpgSessionWorld();w.join(1,"Player A");var before=w.snapshot();
        var im=new java.awt.image.BufferedImage(300,300,2);var g=im.createGraphics();
        try{for(var direction:Direction.values())for(int frame=0;frame<4;frame++){assertTrue(sprites.drawFrame(g,direction,frame,true,150,150));var bounds=sprites.visualBounds(direction,frame,true,150,150);assertEquals(150,bounds.getMaxY(),1);assertTrue(bounds.getWidth()>0);assertEquals(before,w.snapshot());}}finally{g.dispose();}
    }
    @Test void portalArrivalRejectsPlayerUntilGroundBecomesFree(){
        var w=new RpgSessionWorld(List.of("spirit","reaper"),null,true);w.join(1,"Player A");w.join(2,"Player B");
        assertTrue(w.request(1,new Protocol.WorldRequest(1,Protocol.WorldAction.TRAVEL,"to_village")));
        for(int i=1;i<=7;i++){w.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,0,-1));w.step();}
        assertFalse(w.request(2,new Protocol.WorldRequest(8,Protocol.WorldAction.TRAVEL,"to_village")));assertEquals("forest",w.region(2));
        for(int i=2;i<8;i++){w.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,-1,0));w.step();}
        assertTrue(w.request(2,new Protocol.WorldRequest(9,Protocol.WorldAction.TRAVEL,"to_village")));var p=w.snapshot().actors();assertTrue(Math.hypot(p.get(0).x()-p.get(1).x(),p.get(0).y()-p.get(1).y())>=24);
    }
    @Test void playerRespawnRefusesAnOccupiedHome(){
        var w=new RpgSessionWorld(List.of("reaper","spirit"),null,true);w.join(1,"Player A");for(int i=0;i<1200;i++)w.step();assertEquals(0,w.snapshot().actors().get(0).hp());
        w.join(2,"Player B");for(int i=1;i<=80;i++){if(i%12==1)w.accept(2,new Protocol.Intent(i,Protocol.Action.ATTACK,0,0));w.step();}
        assertEquals(0,w.combatSnapshot().session().world().actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow().hp());
        for(int i=100;i<115;i++){w.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,0,-1));w.step();}
        assertFalse(w.request(1,new Protocol.WorldRequest(1,Protocol.WorldAction.RESPAWN,"home")));assertEquals(0,w.snapshot().actors().get(0).hp());
        for(int i=120;i<126;i++){w.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,-1,0));w.step();}
        assertTrue(w.request(1,new Protocol.WorldRequest(2,Protocol.WorldAction.RESPAWN,"home")));assertTrue(w.snapshot().actors().get(0).hp()>0);
    }
}
