package com.projetok.rpg;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Task178ShunpoTest {
    @Test void sceneryClearanceDoesNotIncreaseCombatOcclusionOrChangeRange(){
        var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(RpgSessionGeometry.raw(),new Position(375,312))),Map.of("test_slime","slime|forest|375|160"),Map.of(),Map.of(),Map.of());
        var world=new RpgWorld(CharacterIdentity.create("Player A","spirit","azure"),scenes);world.useNetworkFootprint();
        assertEquals(180,world.basicAttackRange());assertTrue(world.attack());assertEquals(22,world.snapshot().enemies().get(0).hp());
    }
    @Test void shunpoAndEvadeRespectTheSceneryCapsuleAndThinWall(){
        var raw=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(180,100,1,400)),List.of());
        var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(raw,new Position(140,300))),Map.of("test_slime","slime|forest|220|300"),Map.of(),Map.of(),Map.of());
        var world=new RpgWorld(CharacterIdentity.create("Player A","reaper","azure"),scenes);world.useNetworkFootprint();world.awardExperience(RpgProgression.builtin().threshold(20));
        var before=world.save();assertFalse(world.castAbility());assertEquals(before.x(),world.save().x());assertEquals(before.flags(),world.save().flags());
        world.tick(-1,0);assertTrue(world.castSecondaryAbility());assertEquals(158,world.snapshot().x(),.001);assertEquals(300,world.snapshot().y());
        assertEquals(12,world.physicalRadius());
    }
}
