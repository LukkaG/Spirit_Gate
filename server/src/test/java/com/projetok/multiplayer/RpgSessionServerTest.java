package com.projetok.multiplayer;

import java.net.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Backend RPG e transporte real funcionam sem Swing, contas ou saves offline. */
class RpgSessionServerTest {
    @Test void realServerRunsTwoRpgProfilesAndCloses()throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld())){server.start();try(var a=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador A");var b=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador B")){
            a.await(s->s.actors().size()==2,Duration.ofSeconds(3));assertTrue(a.send(Protocol.Action.MOVE,1,0));b.await(s->s.actors().stream().anyMatch(v->v.id()==a.id()&&v.x()>140),Duration.ofSeconds(3));assertNull(server.failure());
        }server.close();assertFalse(server.running());}
    }
    @Test void backendValidatesCapacitySequenceAndSyntheticRaces(){var world=new RpgSessionWorld();assertTrue(world.join(1,"Jogador A"));assertTrue(world.join(2,"Jogador B"));assertFalse(world.join(3,"Jogador C"));assertEquals("reaper",world.race(1));assertEquals("human",world.race(2));assertTrue(world.accept(1,new Protocol.Intent(2,Protocol.Action.MOVE,1,0)));assertFalse(world.accept(1,new Protocol.Intent(2,Protocol.Action.MOVE,-1,0)));assertFalse(world.accept(1,new Protocol.Intent(3,Protocol.Action.MOVE,999,0)));assertFalse(world.accept(1,new Protocol.Intent(4,Protocol.Action.ATTACK,0,0)));world.leave(1);assertFalse(world.accept(1,new Protocol.Intent(5,Protocol.Action.MOVE,1,0)));}
}
