package com.projetok.multiplayer;

import com.projetok.studio.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Valida seleção e suporte por TCP real, incluindo derrota causada pela IA autoritativa. */
class AllyTargetSupportServerTest {
    private static LocalClient client(LocalSimulationServer server,String name)throws Exception{
        var value=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),
                new SessionCharacterSelection(name,"human","none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
        value.revisions().renderer(p->CompletableFuture.completedFuture(null));return value;
    }
    private static Protocol.Actor actor(LocalClient client,int id){
        return client.snapshot().actors().stream().filter(a->a.id()==id).findFirst().orElseThrow();
    }
    private static RpgSessionWorld world(LiveRevision revision){
        var world=new RpgSessionWorld(List.of("human","human"),null,true,20);world.enableRevisions(revision);return world;
    }
    private static boolean ready(LocalClient value){return value.snapshot()!=null&&value.playerSheet()!=null&&value.skills()!=null&&!value.revisions().blocked();}

    /** O alvo aliado não converte o ataque em um golpe livre contra o mob próximo. */
    @Test void selectingAllyRejectsOffensiveFallbackAndNpcTargets()throws Exception{
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),185,300):e).toList()));
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY)))){
            server.start();try(var a=client(server,"Alvo Principal");var b=client(server,"Alvo Aliado")){
                await(server,()->ready(a)&&ready(b),7,a,b);
                int enemy=a.snapshot().actors().stream().filter(v->v.id()>1_000_000&&v.id()<2_000_000).findFirst().orElseThrow().id();
                int hp=actor(a,enemy).hp();assertTrue(a.selectTarget(b.id()));assertEquals(b.id(),a.selectedTarget());
                assertFalse(a.selectTarget(2_000_001));
                assertTrue(a.gameplay(GameplayProtocol.Operation.BASIC_ATTACK,"none"));
                assertTrue(a.gameplay(GameplayProtocol.Operation.SHUNPO_OFFENSIVE,"none"));
                long tick=a.snapshot().tick();await(server,()->a.snapshot().tick()>tick+5,7,a,b);
                assertEquals(hp,actor(a,enemy).hp());assertEquals(0,a.playerSheet().abilities().shunpoTicks());
                assertEquals(b.id(),a.selectedTarget());assertTrue(a.selectTarget(0));assertEquals(0,a.selectedTarget());
                assertNull(server.failure());
            }
        }
    }

    /** Com vida cheia, cura recusada não cria recarga, consumo nem animação de acerto fictício. */
    @Test void fullHealthHealingRefusesWithoutCostOrCooldown()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world(WorldContent.baseline()))){
            server.start();try(var healer=client(server,"Luz Completa")){
                await(server,()->ready(healer),7,healer);
                healer.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"light");
                await(server,()->"light".equals(healer.playerSheet().progress().domain()),7,healer);
                assertTrue(healer.selectTarget(healer.id()));assertTrue(healer.castSkill(1,healer.id(),0,0,0));
                long tick=healer.snapshot().tick();await(server,()->healer.snapshot().tick()>tick+4,7,healer);
                assertEquals(0,healer.skills().cooldowns().get(0));assertEquals(1000,actor(healer,healer.id()).stamina());
                assertTrue(healer.skills().events().stream().noneMatch(e->e.skill().equals("light_1")));
            }
        }
    }

    /** Cinco lobos permitem testar cura e revive por combate verdadeiro, preservando o limite global com o NPC da vila. */
    @Test void selectedAllyReceivesHealingAndReviveAndLightCanDamageAMob()throws Exception{
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        var seed=forest.elements().stream().filter(e->e.kind()==WorldDocument.Kind.ENEMY).findFirst().orElseThrow();
        var elements=new ArrayList<>(forest.elements().stream().filter(e->e.kind()!=WorldDocument.Kind.ENEMY).toList());
        for(int i=0;i<5;i++)elements.add(new WorldDocument.Element("wolf_"+i,WorldDocument.Kind.ENEMY,
                seed.asset(),seed.sha(),seed.crop(),140,456,seed.width(),seed.height(),seed.layer(),
                seed.visible(),seed.locked(),"wolf",seed.direction(),seed.properties()));
        maps.put("forest",forest.withElements(elements));
        var revision=LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY);
        // Falha de autoria deve aparecer antes do handshake, que oferece o conteúdo completo das duas regiões.
        assertDoesNotThrow(()->WorldContent.encode(revision));
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world(revision))){
            server.start();try(var healer=client(server,"Luz Suporte");var ally=client(server,"Aliado Ferido")){
                await(server,()->ready(healer)&&ready(ally),7,healer,ally);
                healer.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"light");
                await(server,()->"light".equals(healer.playerSheet().progress().domain()),7,healer,ally);
                assertTrue(healer.selectTarget(ally.id()));
                await(server,()->actor(healer,ally.id()).hp()<actor(healer,ally.id()).maxHp(),7,healer,ally);
                int injured=actor(healer,ally.id()).hp();assertTrue(healer.castSkill(1,ally.id(),0,0,0));
                await(server,()->healer.skills().cooldowns().get(0)>0&&actor(healer,ally.id()).hp()>injured,7,healer,ally);
                int beforeArea=actor(healer,ally.id()).hp();assertTrue(healer.castSkill(2,0,0,0,0));
                await(server,()->healer.skills().cooldowns().get(1)>0&&actor(healer,ally.id()).hp()>beforeArea,7,healer,ally);
                int enemyId=healer.snapshot().actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000&&a.hp()>0).findFirst().orElseThrow().id();
                int enemyHp=actor(healer,enemyId).hp();assertTrue(healer.selectTarget(enemyId));
                assertTrue(healer.castSkill(5,enemyId,0,0,0));
                await(server,()->actor(healer,enemyId).hp()<enemyHp,7,healer,ally);
                assertTrue(healer.selectTarget(ally.id()));
                await(server,()->actor(healer,ally.id()).hp()==0,24,healer,ally);
                assertEquals(ally.id(),healer.selectedTarget(),"Aliado derrotado deve continuar selecionado para revive");
                assertTrue(healer.castSkill(3,ally.id(),0,0,0));
                await(server,()->healer.skills().cooldowns().get(2)>0&&actor(healer,ally.id()).hp()>0,7,healer,ally);
                assertTrue(actor(ally,ally.id()).hp()>0);assertTrue(healer.connected()&&ally.connected());assertNull(server.failure());
            }
        }
    }

    private static void await(LocalSimulationServer server,Task201CharacterSessionTest.Check check,int seconds,LocalClient... clients)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds),ping=0;
        while(System.nanoTime()<deadline){
            assertNull(server.failure());for(var client:clients)assertTrue(client.connected(),client.problem());
            if(check.get())return;
            if(System.nanoTime()>ping){for(var client:clients)client.measureLatency();ping=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(500);}
            Thread.sleep(15);
        }
        assertTrue(check.get(),"Confirmação autoritativa de suporte não recebida");
    }
}
