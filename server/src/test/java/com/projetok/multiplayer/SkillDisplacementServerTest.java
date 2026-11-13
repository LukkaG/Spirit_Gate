package com.projetok.multiplayer;

import com.projetok.studio.LiveRevision;
import com.projetok.studio.WorldContent;
import com.projetok.studio.WorldDocument;
import java.net.InetAddress;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regressão TCP do body slam em contato diagonal permitido pelos círculos de admissão. */
class SkillDisplacementServerTest {
    /** Mantém a cena real e desloca somente o marcador do mob para o contato que provocava o crash. */
    private static LiveRevision diagonalContact()throws Exception {
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        var elements=forest.elements().stream().map(element->element.kind()==WorldDocument.Kind.ENEMY
                ?element.at(element.id(),165,325):element).toList();
        maps.put("forest",forest.withElements(elements));
        return LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY);
    }

    /** Usa clientes V7 reais e o renderer sem UI, preservando negociação, sequência e revisão. */
    private static LocalClient connect(LocalSimulationServer server,String name)throws Exception {
        var client=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),
                new SessionCharacterSelection(name,"human","none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(prepared->CompletableFuture.completedFuture(null));return client;
    }

    @Test void bodySlamDoesNotCrashTwoTcpClientsAtLegalDiagonalCircleContact()throws Exception {
        var revision=diagonalContact();float sum=revision.profile().actorRadius()+19;
        assertTrue(Math.hypot(25,25)>sum,"A admissão circular precisa aceitar o contato diagonal");
        assertTrue(25<sum,"O mesmo contato reproduz a origem inválida do bloco expandido");
        var world=new RpgSessionWorld(List.of("human","human"),null,true,20);world.enableRevisions(revision);
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();
            try(var martial=connect(server,"Marcial Diagonal");var observer=connect(server,"Observador Diagonal")){
                await(server,()->ready(martial)&&ready(observer),martial,observer);
                assertTrue(martial.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"martial"));
                await(server,()->martial.playerSheet()!=null&&"martial".equals(martial.playerSheet().progress().domain()),martial,observer);
                var player=self(martial);var before=enemy(martial);int enemyId=before.id();
                assertEquals(140,player.x(),.01);assertEquals(300,player.y(),.01);
                assertEquals(165,before.x(),.01);assertEquals(325,before.y(),.01);
                assertTrue(martial.selectTarget(enemyId));assertTrue(martial.castSkill(4,enemyId,0,0,0));
                await(server,()->martial.skills()!=null&&martial.skills().events().stream().anyMatch(event->event.skill().equals("martial_4"))
                        &&enemy(martial).hp()<before.hp(),martial,observer);
                var event=martial.skills().events().stream().filter(value->value.skill().equals("martial_4")).findFirst().orElseThrow();
                var impact=enemy(martial);assertTrue(impact.hp()>0,"O mob precisa sobreviver para verificar o stun");
                assertTrue(impact.hp()<=before.hp()-34,"O golpe deve aplicar o dano declarado");
                await(server,()->observer.skills()!=null&&observer.skills().events().stream().anyMatch(value->value.skill().equals("martial_4")),martial,observer);
                // Fora da distância de ataque, um mob sem stun começaria a perseguir o jogador.
                assertTrue(martial.send(Protocol.Action.MOVE,-1,0));
                await(server,()->self(martial).x()<player.x()-30,martial,observer);
                assertTrue(martial.send(Protocol.Action.MOVE,0,0));
                await(server,()->martial.snapshot().tick()>=event.tick()+20,martial,observer);
                var stunned=enemy(martial);
                assertEquals(impact.x(),stunned.x(),.01,"Stun deve impedir a perseguição após o deslocamento");
                assertEquals(impact.y(),stunned.y(),.01,"Stun deve impedir a perseguição após o deslocamento");
                assertTrue(Math.hypot(stunned.x()-self(martial).x(),stunned.y()-self(martial).y())>46);
                await(server,()->martial.snapshot().tick()>=event.tick()+40
                        &&Math.hypot(enemy(martial).x()-impact.x(),enemy(martial).y()-impact.y())>1,martial,observer);
                long tick=martial.snapshot().tick();
                await(server,()->martial.snapshot().tick()>tick+3&&observer.snapshot()!=null&&observer.snapshot().tick()>tick+3,martial,observer);
                assertTrue(martial.connected());assertTrue(observer.connected());assertNull(server.failure());assertTrue(server.running());
                assertEquals(enemy(martial).hp(),enemy(observer).hp(),"Ambos os clientes devem continuar recebendo o mesmo resultado");
            }
        }
    }

    private static boolean ready(LocalClient client){return client.connected()&&!client.revisions().blocked()&&client.snapshot()!=null&&client.playerSheet()!=null&&client.skills()!=null;}
    private static Protocol.Actor self(LocalClient client){return client.snapshot().actors().stream().filter(actor->actor.id()==client.id()).findFirst().orElseThrow();}
    private static Protocol.Actor enemy(LocalClient client){return client.snapshot().actors().stream().filter(actor->actor.id()>1_000_000&&actor.id()<2_000_000).findFirst().orElseThrow();}
    /** Falha imediatamente se a simulação morrer, conservando a exceção real como evidência. */
    private static void await(LocalSimulationServer server,Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(System.nanoTime()<deadline){
            assertNull(server.failure(),"A simulação encerrou durante o deslocamento de habilidade");
            for(var client:clients){assertTrue(client.connected(),client.problem());client.measureLatency();}
            if(condition.get())return;Thread.sleep(10);
        }
        assertNull(server.failure());assertTrue(condition.get(),"O estado TCP esperado não foi recebido no prazo");
    }
}
