package com.projetok.multiplayer;

import com.projetok.studio.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Confirma por TCP a troca básico frontal/target, imobilidade e cancelamento da Sentinela. */
class SentryRefinementServerTest {
    @Test void sentryConvertsOnlyTheActiveWindowAndFourWayMovementStaysAuthoritative()throws Exception{
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),280,300):e).toList()));
        var world=new RpgSessionWorld(List.of("aetheri","human"),null,true,20);
        world.enableRevisions(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY));
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var client=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),
                    new SessionCharacterSelection("Arqueiro Sentinela","aetheri","none","none"),false,false,null,Task201CharacterSessionTest.EMPTY)){
                client.revisions().renderer(p->CompletableFuture.completedFuture(null));
                waitFor(()->client.playerSheet()!=null&&client.skills()!=null&&!client.revisions().blocked(),client);
                client.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"bow_long");
                waitFor(()->client.playerSheet().progress().domain().equals("bow_long"),client);
                assertTrue(client.castSkill(2,0,0,0,0));waitFor(()->client.skills().sentryShots()>0,client);
                var origin=self(client);long tick=client.snapshot().tick();
                assertTrue(client.send(Protocol.Action.MOVE,0,1));
                waitFor(()->client.snapshot().tick()>tick+4,client);
                assertEquals(origin.x(),self(client).x(),.01);assertEquals(origin.y(),self(client).y(),.01);
                assertTrue(client.send(Protocol.Action.MOVE,0,0));
                int enemy=client.snapshot().actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000).findFirst().orElseThrow().id();
                assertTrue(client.selectTarget(enemy));assertTrue(client.send(Protocol.Action.ATTACK,0,0));
                waitFor(()->client.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_2")&&e.fromX()!=e.x()),client);
                assertTrue(client.castSkill(2,0,0,0,0));waitFor(()->client.skills().sentryShots()==0,client);
                assertTrue(client.skills().cooldowns().get(1)>0);
                assertTrue(client.send(Protocol.Action.MOVE,0,1));waitFor(()->self(client).facingY()>0,client);
                assertTrue(client.send(Protocol.Action.MOVE,0,0));
                assertTrue(client.send(Protocol.Action.ATTACK,0,0));
                waitFor(()->client.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_1")&&e.y()>e.fromY()),client);
                var before=self(client);assertTrue(client.send(Protocol.Action.MOVE,1,1));
                waitFor(()->self(client).y()>before.y()+5,client);assertTrue(client.send(Protocol.Action.MOVE,0,0));
                assertEquals(before.x(),self(client).x(),.01,"Aplicação 7 não deve caminhar em diagonal");assertNull(server.failure());
            }
        }
    }
    private static Protocol.Actor self(LocalClient client){return client.snapshot().actors().stream().filter(a->a.id()==client.id()).findFirst().orElseThrow();}
    private static void waitFor(Task201CharacterSessionTest.Check check,LocalClient client)throws Exception{Task201CharacterSessionTest.waitFor(check,client);}
}
