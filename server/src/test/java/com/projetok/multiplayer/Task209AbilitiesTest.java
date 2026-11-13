package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task209AbilitiesTest {
    @Test void twoTcpPlayersSeeSharedShunpoCooldownAndServerValidatesDomain()throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);
        world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)) {
            server.start();try(var a=Task207InventoryTest.connect(server,"Shunpo Reaper","reaper");var b=Task207InventoryTest.connect(server,"Shunpo Human","human")) {
                Task201CharacterSessionTest.ready(a,b);
                Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                assertEquals(20,a.playerSheet().progress().level());
                assertTrue(a.cycleTarget());
                a.send(Protocol.Action.LIGHTNING_STEP,0,0);
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().abilities().shunpoTicks()>0,a,b);
                assertTrue(a.snapshot().actors().stream().filter(p->p.id()==a.id()).findFirst().orElseThrow().stamina()<1000);
                float x=a.snapshot().actors().stream().filter(p->p.id()==a.id()).findFirst().orElseThrow().x();
                b.await(s->s.actors().stream().anyMatch(p->p.id()==a.id()&&Math.abs(p.x()-x)<.01),Duration.ofSeconds(3));
                a.send(Protocol.Action.CRYOSTASIS,0,0);
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"light");long tick=a.snapshot().tick();
                a.await(s->s.tick()>tick+3,Duration.ofSeconds(3));assertEquals("none",a.playerSheet().progress().domain());
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"water");
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("water"),a,b);
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"fire");long until=a.snapshot().tick()+3;
                a.await(s->s.tick()>until,Duration.ofSeconds(3));assertEquals("water",a.playerSheet().progress().domain());assertEquals("none",b.playerSheet().progress().domain());
                assertNull(server.failure());
            }
        }
    }
}
