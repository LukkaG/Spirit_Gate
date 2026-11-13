package com.projetok.multiplayer;

import com.projetok.studio.LiveRevision;
import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Prova TCP da travessia do corpo invisível e do retorno seguro em dois clientes. */
class LightningFieldServerTest {
    @Test void allyCrossesInvisibleCasterAndBothClientsReceiveTheEnd()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),null,true,20);
        world.enableRevisions(LiveRevision.prepare(0,"forest",WorldContent.baseline().bundle().maps(),Task201CharacterSessionTest.EMPTY));
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var caster=connect(server,"Raio Fase","reaper");var ally=connect(server,"Aliado Fase","human")){
                waitFor(()->ready(caster)&&ready(ally),caster,ally);
                assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder"));
                waitFor(()->caster.playerSheet().progress().domain().equals("thunder"),caster,ally);
                assertTrue(caster.castSkill(5,0,0,0,0));
                waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals("thunder_5")),caster,ally);
                long start=caster.skills().events().stream().filter(e->e.skill().equals("thunder_5")).findFirst().orElseThrow().tick();
                int hp=self(caster).hp();assertTrue(ally.send(Protocol.Action.MOVE,0,-1));
                waitFor(()->self(ally).y()<=self(caster).y()+8,caster,ally);
                assertTrue(ally.send(Protocol.Action.MOVE,0,0));
                assertTrue(caster.snapshot().tick()<start+32);
                assertTrue(Math.abs(self(ally).y()-self(caster).y())<24,"O aliado deve ocupar o corpo intangível");
                assertEquals(hp,self(caster).hp());
                waitFor(()->ally.skills().events().stream().anyMatch(e->e.skill().equals("thunder_5")),caster,ally);
                for(var client:List.of(caster,ally)){
                    var view=new NetworkSkillPresentation();view.update(client.skills());assertTrue(view.hidden(caster.id()));
                }
                waitFor(()->caster.snapshot().tick()>start+33&&ally.snapshot().tick()>start+33,caster,ally);
                for(var client:List.of(caster,ally)){
                    assertTrue(client.skills().events().stream().anyMatch(e->e.skill().equals("thunder_5")&&e.fromX()==e.x()&&e.fromY()==e.y()));
                    var view=new NetworkSkillPresentation();view.update(client.skills());assertFalse(view.hidden(caster.id()));
                }
                assertTrue(Math.abs(self(caster).x()-self(ally).x())>=24-.01||Math.abs(self(caster).y()-self(ally).y())>=24-.01);
                assertNull(server.failure());assertTrue(caster.connected()&&ally.connected());
            }
        }
    }
    private static boolean ready(LocalClient c){return c.snapshot()!=null&&c.skills()!=null&&c.playerSheet()!=null&&!c.revisions().blocked();}
    private static Protocol.Actor self(LocalClient c){return c.snapshot().actors().stream().filter(a->a.id()==c.id()).findFirst().orElseThrow();}
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{
        var c=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
        c.revisions().renderer(prepared->CompletableFuture.completedFuture(null));return c;
    }
    private static void waitFor(Task201CharacterSessionTest.Check check,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(check,clients);}
}
