package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task206SharedLootTest {
    @Test void activeContributorsReceiveCatalogLootOnlyOnceAndDisabledRewardsStayDisabled() {
        for(var policy:SharedRpgEncounter.RewardPolicy.values()) {
            var space=new CollisionService.Space(960,640,List.of(),List.of());
            var encounter=new SharedRpgEncounter(space,policy);
            var scene=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(space,new Position(170,300))),Map.of(),Map.of(),Map.of(),Map.of());
            var world=new RpgWorld(CharacterIdentity.create("Esp Loot","spirit","azure"),scene);
            encounter.join(1,world);
            for(int i=0;i<80&&encounter.enemies(i).get(0).hp()>0;i++) {
                world.tick(0,0);encounter.step(i);world.attack();
            }
            assertEquals(0,encounter.enemies(80).get(0).hp());
            int items=world.inventory().count("crystal"),coins=world.networkState().coins();
            if(policy==SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS){assertTrue(items>=1&&items<=2);assertEquals(5,coins);}
            else {assertEquals(0,items);assertEquals(0,coins);}
            for(int i=0;i<10;i++)assertFalse(world.attack());
            assertEquals(items,world.inventory().count("crystal"));assertEquals(coins,world.networkState().coins());
        }
    }
}
