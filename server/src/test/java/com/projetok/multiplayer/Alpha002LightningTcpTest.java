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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Dois clientes TCP reais em loopback; sem alteração visual, persistência ou retirada injetada. */
@Timeout(25)
class Alpha002LightningTcpTest {
    /** Conteúdo preparado em memória: alvo no círculo e espaço para dois participantes. */
    private static LiveRevision scene() throws Exception {
        var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());
        var forest=maps.get("forest");
        var spawn=forest.elements().stream().filter(e->e.kind()==WorldDocument.Kind.PLAYER).findFirst().orElseThrow();
        maps.put("forest",forest.withHitboxes(List.of()).withElements(forest.elements().stream()
                .map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),spawn.x()+90,spawn.y()):e).toList()));
        return LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY);
    }
    /** Cliente headless conserva negociação e transporte reais, substituindo apenas preparação visual. */
    private static LocalClient connect(LocalSimulationServer server,String name,int application)throws Exception {
        var selection=new SessionCharacterSelection(name,"reaper","none","none");
        var client=application==7?LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),selection,
                false,false,null,Task201CharacterSessionTest.EMPTY):LocalClient.gameplay(InetAddress.getLoopbackAddress(),
                server.port(),selection,false,false,null,Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(prepared->CompletableFuture.completedFuture(null));return client;
    }
    /** Toda espera tem limite e observa falha do owner. */
    private static void await(LocalSimulationServer server,Task201CharacterSessionTest.Check condition,LocalClient...clients)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(System.nanoTime()<end){
            assertNull(server.failure());for(var c:clients){assertTrue(c.connected(),c.problem());c.measureLatency();}
            if(condition.get())return;Thread.sleep(10);
        }
        assertTrue(condition.get(),"Estado TCP não chegou no limite externo");
    }
    private static boolean ready(LocalClient c){return !c.revisions().blocked()&&c.snapshot()!=null&&c.playerSheet()!=null&&(!c.skillApplication()||c.skills()!=null);}
    private static Protocol.Actor actor(LocalClient c,int id){return c.snapshot().actors().stream().filter(a->a.id()==id).findFirst().orElseThrow();}
    private static List<SkillProtocol.Event> events(LocalClient c,int id){return c.skills().events().stream().filter(e->e.player()==id&&e.skill().equals("thunder_5")).toList();}
    private static boolean terminal(SkillProtocol.Event e){return e.fromX()==e.x()&&e.fromY()==e.y();}

    @ParameterizedTest @ValueSource(ints={6,7})
    void normalCompletionKeepsBodySessionAndObserverWithoutExtraAttacks(int application)throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","reaper"),null,true,20);world.enableRevisions(scene());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var caster=connect(server,"Raio Autoritativo",7);var observer=connect(server,"Observador",application)){
                await(server,()->ready(caster)&&ready(observer),caster,observer);
                assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder"));
                await(server,()->"thunder".equals(caster.playerSheet().progress().domain()),caster,observer);
                int id=caster.id();var origin=actor(caster,id);var peerOrigin=actor(observer,observer.id());
                var progress=caster.playerSheet().progress();var inventory=caster.playerSheet().inventory();
                assertTrue(caster.castSkill(5,0,0,0,0));
                await(server,()->!events(caster,id).isEmpty(),caster,observer);
                var start=events(caster,id).get(0);assertFalse(terminal(start));
                assertEquals(origin.x(),actor(caster,id).x());assertEquals(origin.y(),actor(caster,id).y());
                assertTrue(caster.send(Protocol.Action.MOVE,1,0));
                assertTrue(caster.send(Protocol.Action.ATTACK,0,0));
                assertTrue(caster.castSkill(5,0,0,0,0));
                assertTrue(observer.send(Protocol.Action.MOVE,0,1));
                await(server,()->events(caster,id).stream().anyMatch(Alpha002LightningTcpTest::terminal)
                        &&observer.combatEvents().stream().filter(e->e.player()==id&&e.kind().equals("HIT")).count()==4,caster,observer);
                var end=events(caster,id).stream().filter(Alpha002LightningTcpTest::terminal).findFirst().orElseThrow();
                assertEquals(32,end.tick()-start.tick());assertEquals(2,events(caster,id).size());
                assertEquals(origin.x(),actor(caster,id).x());assertEquals(origin.y(),actor(caster,id).y());
                assertNotEquals(peerOrigin.y(),actor(observer,observer.id()).y());
                assertEquals(progress,caster.playerSheet().progress());assertEquals(inventory,caster.playerSheet().inventory());
                assertTrue(caster.skills().cooldowns().get(4)>0);assertEquals(id,caster.id());
                assertEquals(origin.x(),actor(observer,id).x());assertEquals(origin.y(),actor(observer,id).y());
                if(application==7)await(server,()->events(observer,id).stream().anyMatch(Alpha002LightningTcpTest::terminal),caster,observer);
                long until=end.tick()+40;
                await(server,()->caster.snapshot().tick()>until,caster,observer);
                assertEquals(2,events(caster,id).size());assertEquals(origin.x(),actor(caster,id).x());
            }
        }
    }

    @Test void disconnectDuringExecutionDoesNotLeaveLateHitsOrBlockObserver()throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","reaper"),null,true,20);world.enableRevisions(scene());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var caster=connect(server,"Saida",7);var observer=connect(server,"Presente",7)){
                await(server,()->ready(caster)&&ready(observer),caster,observer);
                caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder");
                await(server,()->"thunder".equals(caster.playerSheet().progress().domain()),caster,observer);
                int id=caster.id();caster.castSkill(5,0,0,0,0);
                await(server,()->!events(observer,id).isEmpty(),caster,observer);
                long start=observer.snapshot().tick();caster.close();
                await(server,()->observer.snapshot().tick()>start+40&&observer.snapshot().actors().stream().noneMatch(a->a.id()==id),observer);
                assertTrue(observer.combatEvents().stream().noneMatch(e->e.player()==id&&e.kind().equals("HIT")));
                assertTrue(events(observer,id).isEmpty());
            }
        }
    }
}
