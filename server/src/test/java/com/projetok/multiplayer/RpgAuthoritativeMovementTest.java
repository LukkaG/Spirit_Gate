package com.projetok.multiplayer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Colisão, magnitude e ações recusadas são decididas pelo servidor RPG. */
class RpgAuthoritativeMovementTest {
    Protocol.Actor actor(RpgSessionWorld world,int id){return world.snapshot().actors().stream().filter(a->a.id()==id).findFirst().orElseThrow();}
    @Test void wallsAndMapLimitsBlockEveryMovementIntent(){var world=new RpgSessionWorld();world.join(1,"Jogador A");for(int i=1;i<=150;i++){world.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,1,0));world.step();}assertTrue(actor(world,1).x()<=362);assertTrue(actor(world,1).x()>340);assertEquals(300,actor(world,1).y());}
    @Test void playerBodiesDoNotOverlapAndInvalidVelocityIsRejected(){var world=new RpgSessionWorld();world.join(1,"Jogador A");world.join(2,"Jogador B");assertFalse(world.accept(2,new Protocol.Intent(1,Protocol.Action.MOVE,500,0)));for(int i=2;i<=60;i++){world.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,0,-1));world.step();}var a=actor(world,1);var b=actor(world,2);assertTrue(Math.hypot(a.x()-b.x(),a.y()-b.y())>=36-.001);}
    @Test void unsupportedCombatAndShunpoCannotGrantDamageCoinsOrMovement(){var world=new RpgSessionWorld();world.join(1,"Jogador A");var before=world.snapshot();long seq=0;for(var action:new Protocol.Action[]{Protocol.Action.ATTACK,Protocol.Action.LIGHTNING_STEP,Protocol.Action.CRYOSTASIS})assertFalse(world.accept(1,new Protocol.Intent(++seq,action,700,300)));assertEquals(before,world.snapshot());}
}
