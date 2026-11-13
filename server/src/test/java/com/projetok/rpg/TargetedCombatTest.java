package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetedCombatTest {
    @Test void forwardShunpoFollowsFacingAndRespectsCollisionAndSharedCooldown(){
        var w=world(Map.of(),List.of());w.awardExperience(RpgProgression.builtin().threshold(20));var before=w.networkState();assertTrue(w.castForwardAbility());assertEquals(before.x()+70,w.networkState().x(),.001);assertEquals(before.y(),w.networkState().y());assertEquals(750,w.networkState().stamina());assertEquals(90,w.abilityView().shunpoTicks());assertFalse(w.castSecondaryAbility());
        var wall=world(Map.of(),List.of(new CollisionService.Obstacle(180,100,1,400)));wall.awardExperience(RpgProgression.builtin().threshold(20));assertTrue(wall.castForwardAbility());assertTrue(wall.networkState().x()+wall.physicalRadius()<=180);assertEquals(750,wall.networkState().stamina());
        var low=world(Map.of(),List.of());assertFalse(low.castForwardAbility());assertEquals(140,low.networkState().x());assertEquals(1000,low.networkState().stamina());
    }
    private RpgWorld world(Map<String,String> enemies,List<CollisionService.Obstacle> obstacles){var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(new CollisionService.Space(960,640,obstacles,List.of()),new Position(140,300))),enemies,Map.of(),Map.of(),Map.of());var w=new RpgWorld(CharacterIdentity.create("Target Reaper","reaper","azure"),scenes);w.useNetworkFootprint();return w;}
    @Test void selectedBasicAttackTurnsWithoutMovingAndDoesNotHitTheNearestInstead(){
        var w=world(Map.of("a_near","slime|forest|185|300","b_selected","slime|forest|140|230"),List.of());var before=w.networkState();var target=w.combatTarget(101);assertNotNull(target);
        assertTrue(w.attackTarget(101,target.generation()));assertEquals(before.x(),w.networkState().x());assertEquals(before.y(),w.networkState().y());assertEquals(0,w.combatFacing().x(),.001);assertEquals(-1,w.combatFacing().y(),.001);
        assertEquals(40,w.snapshot().enemies().stream().filter(e->e.id()==100).findFirst().orElseThrow().hp());assertTrue(w.snapshot().enemies().stream().filter(e->e.id()==101).findFirst().orElseThrow().hp()<40);
        var free=world(Map.of("a_near","slime|forest|185|300"),List.of());assertTrue(free.attack());
    }
    @Test void outOfRangeAndOccludedTargetsCannotConsumeOrHitAnotherEnemy(){
        var w=world(Map.of("a_near","slime|forest|185|300","b_far","slime|forest|140|50"),List.of());w.awardExperience(RpgProgression.builtin().threshold(20));var target=w.combatTarget(101);var before=w.networkState();
        assertFalse(w.attackTarget(101,target.generation()));assertEquals(-1,w.combatFacing().y(),.001);assertFalse(w.castAbilityTarget(101,target.generation()));assertEquals(before.stamina(),w.networkState().stamina());assertEquals(0,w.abilityView().shunpoTicks());assertEquals(before.x(),w.networkState().x());assertEquals(before.y(),w.networkState().y());assertTrue(w.snapshot().enemies().stream().allMatch(e->e.hp()==40));
        var wall=world(Map.of("a_enemy","slime|forest|220|300"),List.of(new CollisionService.Obstacle(180,100,1,400)));wall.awardExperience(RpgProgression.builtin().threshold(20));var blocked=wall.combatTarget(100);assertFalse(wall.castAbilityTarget(100,blocked.generation()));assertFalse(wall.attackTarget(100,blocked.generation()));assertEquals(1000,wall.networkState().stamina());
    }
    @Test void targetedShunpoKeepsCostsAndSharedCooldownAndRejectsOldSpawn(){
        var w=world(Map.of("a_enemy","slime|forest|220|300"),List.of());w.awardExperience(RpgProgression.builtin().threshold(20));var target=w.combatTarget(100);assertTrue(w.castAbilityTarget(100,target.generation()));assertEquals(750,w.networkState().stamina());assertEquals(90,w.abilityView().shunpoTicks());assertFalse(w.castSecondaryAbility());assertEquals(750,w.networkState().stamina());
        assertTrue(w.attackTarget(100,target.generation()));assertNull(w.combatTarget(100));for(int i=0;i<241;i++)w.tick(0,0);var respawn=w.combatTarget(100);assertNotNull(respawn);assertNotEquals(target.generation(),respawn.generation());assertFalse(w.attackTarget(100,target.generation()));assertFalse(w.castAbilityTarget(100,target.generation()));
    }
}
