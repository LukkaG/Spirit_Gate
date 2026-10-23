package com.projetok.multiplayer;

import java.net.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reconexão é nova sessão; a anterior não mantém entidade nem autorização de comando. */
class RpgReconnectIsolationTest {
    @Test void controlledReconnectGetsNewIdAndServerShutdownClosesClient()throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld())){server.start();try(var a=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador A");var b=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador B")){
            a.await(s->s.actors().size()==2,Duration.ofSeconds(3));assertThrows(IllegalStateException.class,a::reconnect);int previous=a.id();a.close();b.await(s->s.actors().size()==1,Duration.ofSeconds(3));assertFalse(a.send(Protocol.Action.MOVE,1,0));try(var next=a.reconnect()){assertNotEquals(previous,next.id());b.await(s->s.actors().size()==2&&s.actors().stream().noneMatch(v->v.id()==previous),Duration.ofSeconds(3));server.close();assertThrows(java.util.concurrent.TimeoutException.class,()->next.await(s->s.tick()==1_000_000_000,Duration.ofSeconds(3)));assertFalse(next.connected());}
        }}
    }
    @Test void publicNetworkNeedsExplicitPrivateLanAuthorization()throws Exception {assertThrows(IllegalArgumentException.class,()->new LocalClient(InetAddress.getByName("8.8.8.8"),12345,"Jogador A"));assertThrows(IllegalArgumentException.class,()->LocalSimulationServer.lan(InetAddress.getByName("0.0.0.0"),12345));}
}
