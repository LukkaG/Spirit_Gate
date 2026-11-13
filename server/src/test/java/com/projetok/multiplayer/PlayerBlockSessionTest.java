package com.projetok.multiplayer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exerce a autoridade real da sessao, incluindo contato e escape em dois jogadores. */
class PlayerBlockSessionTest {
    private Protocol.Actor actor(RpgSessionWorld world,int id){return world.snapshot().actors().stream().filter(a->a.id()==id).findFirst().orElseThrow();}
    private long sequence;
    private void step(RpgSessionWorld world,int id,float x,float y){assertTrue(world.accept(id,new Protocol.Intent(++sequence,Protocol.Action.MOVE,x,y)));world.step();}
    @Test void jogadorEmContatoNaoAtravessaBlocoEConsegueSairNaDiagonal() {
        var world=new RpgSessionWorld();assertTrue(world.join(1,"Aaa"));assertTrue(world.join(2,"Bbb"));
        for(int i=0;i<50;i++)step(world,2,0,-1);
        var fixed=actor(world,1);var stopped=actor(world,2);
        assertEquals(24,stopped.y()-fixed.y(),.0001);
        for(int i=0;i<100;i++)step(world,2,0,-1);
        assertEquals(stopped.x(),actor(world,2).x());assertEquals(stopped.y(),actor(world,2).y());
        for(int i=0;i<20;i++){
            step(world,2,1,-1);
            var moved=actor(world,2);
            assertTrue(Math.abs(moved.x()-fixed.x())>=24||Math.abs(moved.y()-fixed.y())>=24,"Blocos nunca podem se sobrepor");
        }
        assertTrue(actor(world,2).x()>fixed.x()+24);
        assertTrue(actor(world,2).y()<stopped.y());
        assertEquals(fixed,actor(world,1),"Contato nao empurra o amigo");
    }
}
