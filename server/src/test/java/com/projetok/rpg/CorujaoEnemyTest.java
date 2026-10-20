package com.projetok.rpg;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoEnemyTest {
    @Test void patrolDoesNotOvershootAndAttackStateReturnsToChase() {
        var space=new CollisionService.Space(960,640,java.util.List.of(),java.util.List.of());
        var service=new EnemyService(RpgContent.builtin(),space);var e=service.current("forest").get(0);
        e.setPosition(new Position(e.home.x()+23.9f,e.home.y()));
        var movement=new MovementService(new CollisionService());
        service.update("forest",1,new Position(900,600),false,movement,space,n->fail("Ataque sem alvo vivo"),p->true,true);
        assertTrue(Math.abs(e.getPosition().x()-e.home.x())<24);
        e.setPosition(new Position(280,360));e.behavior="ATTACK";
        service.update("forest",2,new Position(360,360),true,movement,space,n->fail("Ataque fora de alcance"),p->true,true);
        assertEquals("CHASE",e.behavior);
        for(int i=0;i<200;i++)service.update("forest",i+3,new Position(900,600),false,movement,space,n->fail("Ataque em retorno"),p->true,true);
        assertEquals(e.home,e.getPosition());assertEquals("PATROL",e.behavior);
    }
    @Test void pursuitCollisionRespawnAndSaveRemainDeterministic() {
        var a=EnemyRpgTest.forest();var b=RpgWorld.restore(a.save());
        for(int i=0;i<500;i++) {
            if(i%12==0){a.attack();b.attack();}
            int x=i<60?-1:0;a.tick(x,0);b.tick(x,0);
            if(i%50==0)b=RpgWorld.restore(b.save());
            assertEquals(a.save(),b.save());
        }
    }
}
