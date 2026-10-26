package com.projetok.multiplayer;
import com.projetok.prototype.rpg.*;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class LauncherSessionTest {
    @Test void launcherFactoryOwnsOnlyItsListenerAndAdmitsCombatClients()throws Exception {
        var ip=InetAddress.getLoopbackAddress();
        try(var external=new ServerSocket(0,2,ip)) {
            assertThrows(BindException.class,()->LauncherSession.start(ip,external.getLocalPort(),null));
            assertFalse(external.isClosed());
            int port;
            try(var server=LauncherSession.start(ip,0,null)) {
                port=server.port();
                try(var a=LocalClient.combat(ip,port,"Viajante A",false);var b=LocalClient.combat(ip,port,"Viajante B",false)) {
                    a.await(s->s.actors().stream().filter(x->!x.boss()).count()==2,Duration.ofSeconds(3));
                    assertTrue(a.supportsCombat());assertEquals(2,server.players());
                    var before=a.snapshot().actors().stream().filter(x->x.id()==a.id()).findFirst().orElseThrow();
                    a.send(Protocol.Action.MOVE,1,0);
                    a.await(s->s.actors().stream().anyMatch(x->x.id()==a.id()&&x.x()>before.x()),Duration.ofSeconds(3));
                }
            }
            try(var rebound=new ServerSocket(port,2,ip)){assertEquals(port,rebound.getLocalPort());}
            assertFalse(external.isClosed());
        }
    }
    @Test void rejectsInvalidEndpoints()throws Exception {
        for(String ip:new String[]{"example.com","26.1.2.999","0::1","26.1.2"})assertThrows(Exception.class,()->RadminSessionPanel.ipv4(ip));
        assertEquals("26.1.2.3",RadminSessionPanel.ipv4("26.1.2.3").getHostAddress());
        assertThrows(IllegalArgumentException.class,()->RadminSessionPanel.port("0"));
        assertThrows(IllegalArgumentException.class,()->RadminSessionPanel.port("65536"));
    }
}
