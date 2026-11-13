package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica carga, alcance e visibilidade dos novos ataques sem alterar o legado. */
class MobCeroTest {
    private final CollisionService.Space space=new CollisionService.Space(960,640,List.of(),List.of());
    @Test void rangedWaitsForChargeAndChecksSightAtRelease() {
        for(String type:List.of("alado","sentinela")) {
            var service=new EnemyService(RpgContent.builtin(),space,Map.of("test","mob_"+type+"|forest|200|300"),r->space);
            int[] damage={0};
            int charge=type.equals("alado")?7:10;
            for(int tick=1;tick<=charge;tick++)step(service,tick,new Position(380,300),true,damage);
            assertEquals(0,damage[0]);assertEquals("PREPARE",service.views("forest").get(0).state());
            step(service,charge+1,new Position(380,300),true,damage);
            assertEquals(7,damage[0]);assertEquals("ATTACK",service.views("forest").get(0).state());
            assertNull(service.views("forest").get(0).threat(),"Cero nao e a ameaca circular do boss");
        }
    }
    @Test void leavingRangeOrOccludingCancelsDamage() {
        for(boolean visible:List.of(false,true)) {
            var service=new EnemyService(RpgContent.builtin(),space,Map.of("test","mob_alado|forest|200|300"),r->space);
            int[] damage={0};step(service,1,new Position(380,300),true,damage);
            step(service,11,new Position(visible?480:380,300),visible,damage);
            assertEquals(0,damage[0]);
        }
    }
    @Test void legacySentinelKeepsMeleeRange() {
        assertFalse(EnemyService.isCero("sentinel"));
        var service=new EnemyService(RpgContent.builtin(),space,Map.of("test","sentinel|ruins|200|300"),r->space);
        int[] damage={0};
        service.update("ruins",1,new Position(380,300),true,new MovementService(new CollisionService()),space,n->damage[0]+=n,q->true,true);
        assertEquals(0,damage[0]);
    }
    private void step(EnemyService service,long tick,Position player,boolean visible,int[] damage) {
        service.update("forest",tick,player,true,new MovementService(new CollisionService()),space,n->damage[0]+=n,q->visible,true);
    }
}
