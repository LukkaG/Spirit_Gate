package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task205EnemyTargetsTest {
    @Test void eachEnemySelectsItsOwnLivingTargetAndReturnsAfterDeparture() {
        var geometry=new CollisionService.Space(960,640,List.of(),List.of());
        var encounter=new SharedRpgEncounter(Map.of("forest",geometry),Map.of("west","slime|forest|200|200","east","slime|forest|760|400"),SharedRpgEncounter.RewardPolicy.DISABLED);
        var first=player("Primeiro",geometry,new Position(170,200));
        var second=player("Segundo",geometry,new Position(790,400));
        encounter.join(1,first);encounter.join(2,second);
        int a=first.networkState().hp(),b=second.networkState().hp();
        encounter.step(1);
        assertTrue(first.networkState().hp()<a);assertTrue(second.networkState().hp()<b);
        assertTrue(encounter.enemies(1).stream().allMatch(e->e.state().equals("ATTACK")));
        encounter.leave(first);encounter.leave(second);encounter.step(2);
        assertTrue(encounter.enemies(2).stream().noneMatch(e->e.state().equals("ATTACK")||e.state().equals("CHASE")));
    }
    private RpgWorld player(String name,CollisionService.Space space,Position spawn) {
        return new RpgWorld(CharacterIdentity.create(name,"human","azure"),
            new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(space,spawn)),Map.of(),Map.of(),Map.of(),Map.of()));
    }
}
