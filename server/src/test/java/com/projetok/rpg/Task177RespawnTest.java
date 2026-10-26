package com.projetok.rpg;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Task177RespawnTest {
    @Test void shunpoDoesNotConsumeResourcesOrEnterAnotherGroundBody(){
        var geometry=new CollisionService.Space(960,640,List.of(),List.of());
        var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(geometry,new Position(140,300))),Map.of("test_slime","slime|forest|220|300"),Map.of(),Map.of(),Map.of());
        var world=new RpgWorld(CharacterIdentity.create("Player A","reaper","azure"),scenes);
        world.useNetworkFootprint();world.awardExperience(RpgProgression.builtin().threshold(20));
        var other=new GameEntity(2,"Player B",new Position(178,300)){};
        world.setSyntheticBodies(List.of(new CollisionService.Body(other,CharacterFootprint.RPG_NETWORK.radius())));
        var before=world.snapshot();assertFalse(world.castAbility());assertEquals(before.x(),world.snapshot().x());assertEquals(before.state().flags(),world.snapshot().state().flags());
        other.setPosition(new Position(178,350));assertTrue(world.castAbility());assertEquals(178,world.snapshot().x(),.001);assertEquals(300,world.snapshot().y(),.001);assertTrue(Math.hypot(world.snapshot().x()-other.getPosition().x(),world.snapshot().y()-other.getPosition().y())>=24);
    }
    @Test void enemyRespawnWaitsUntilGroundIsVacant(){
        var base=new CollisionService.Space(960,640,List.of(),List.of());
        var enemies=new EnemyService(RpgContent.builtin(),base,Map.of("coop_slime","slime|forest|220|300"),r->base);
        var e=enemies.current("forest").get(0);e.hp=0;e.readyTick=1;long generation=e.generation;
        var player=new GameEntity(1,"Player",new Position(220,300)){};
        var occupied=new CollisionService.Space(960,640,List.of(),List.of(new CollisionService.Body(player,CharacterFootprint.RPG_NETWORK.radius())));
        enemies.respawn(100,"forest",occupied);assertEquals(0,e.hp);assertEquals(generation,e.generation);
        player.setPosition(new Position(251,300));enemies.respawn(101,"forest",occupied);assertTrue(e.hp>0);assertEquals(generation+1,e.generation);assertEquals(new Position(220,300),e.getPosition());
    }
}
