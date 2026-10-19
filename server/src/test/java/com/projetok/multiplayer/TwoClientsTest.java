package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class TwoClientsTest {
    @Test void twoDistinctSessionsSeeMovementLeaveAndReconnect()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){server.start();try(var one=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador Um");var two=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador Dois")){
            assertNotEquals(one.id(),two.id());one.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(3));two.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(3));one.send(Protocol.Action.MOVE,1,0);var seen=two.await(s->s.actors().stream().anyMatch(a->a.id()==one.id()&&a.x()>150),Duration.ofSeconds(3));assertEquals(140,seen.actors().stream().filter(a->a.id()==two.id()).findFirst().orElseThrow().x());
            int old=one.id();one.close();two.await(s->s.actors().stream().filter(a->!a.boss()).count()==1,Duration.ofSeconds(3));try(var reconnect=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Retorno")){assertNotEquals(old,reconnect.id());two.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(3));}assertNull(server.failure());
        }}
    }
}
