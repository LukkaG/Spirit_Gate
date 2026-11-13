package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.awt.GraphicsEnvironment;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Dois clientes reais conferem e capturam lançamento, contato, chão e os novos atlas V4. */
class SkillImpactRefinementGuiTest {
    @TempDir Path temporary;
    @ParameterizedTest @ValueSource(strings={"fire","water","martial","aetheri","wind"})
    void confirmedPhasesRenderOnCasterAndObserver(String kit)throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();var evidence=Files.createDirectories(root.resolve("docs/skills-preview-v4/runtime"));
        String race=switch(kit){case "fire","water","wind"->"reaper";case "aetheri"->"aetheri";default->"human";};
        String domain=kit.equals("aetheri")?"bow_long":kit;
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        int targetX=kit.equals("wind")?300:185;
        maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),targetX,300):e).toList()));
        var world=new RpgSessionWorld(List.of(race,"human"),null,true,20);world.enableRevisions(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY));
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var client=connect(server,"Efeito "+kit,race);var observer=connect(server,"Observador V4","human")){
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(client,sprites,"Efeitos V4",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                    frames[1]=RpgNetworkClientLauncher.open(observer,sprites,"Observador V4",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                });
                waitFor(()->ready(client)&&ready(observer),client,observer);client.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,domain);
                waitFor(()->client.playerSheet().progress().domain().equals(domain),client,observer);
                waitFor(()->GamePanelsIntegrationTest.onEdt(()->presentation(frames[0]).clip("water_1.impact")!=null&&presentation(frames[1]).clip("water_1.impact")!=null),client,observer);
                int enemy=client.snapshot().actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000).findFirst().orElseThrow().id();assertTrue(client.selectTarget(enemy));
                if(kit.equals("fire")){
                    final long start=client.snapshot().tick();assertTrue(client.castSkill(2,0,0,0,0));
                    waitFor(()->client.snapshot().tick()>start+8,client,observer);capture(frames[0],evidence,"fissura-parcial");
                    waitFor(()->client.snapshot().tick()>start+16,client,observer);capture(frames[1],evidence,"fissura-chao-observador");
                    final long eruption=client.snapshot().tick();assertTrue(client.castSkill(4,0,0,0,0));
                    waitFor(()->client.snapshot().tick()>eruption+18,client,observer);capture(frames[0],evidence,"fissura-erupcao");
                }else if(kit.equals("aetheri")){
                    assertTrue(client.castSkill(3,0,0,220,300));
                    waitFor(()->client.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_3")),client,observer);
                    long launched=client.skills().events().stream().filter(e->e.skill().equals("aetheri_3")).findFirst().orElseThrow().tick();
                    waitFor(()->client.snapshot().tick()>=launched+6,client,observer);capture(frames[0],evidence,"chuva-em-queda");
                    waitFor(()->client.snapshot().tick()>=launched+13,client,observer);capture(frames[1],evidence,"chuva-impacto-chao");
                }else if(kit.equals("martial")){
                    assertTrue(client.castSkill(1,enemy,0,0,0));waitFor(()->client.skills().cooldowns().get(0)>0,client,observer);capture(frames[0],evidence,"soco-detalhado");
                    assertTrue(client.castSkill(2,enemy,0,0,0));waitFor(()->client.skills().events().stream().anyMatch(e->e.skill().equals("martial_2")),client,observer);capture(frames[1],evidence,"sequencia-socos");
                    assertTrue(client.castSkill(3,0,0,0,0));long start=client.snapshot().tick();waitFor(()->client.snapshot().tick()>start+6,client,observer);capture(frames[0],evidence,"sismico-resolucao-nativa");
                }else if(kit.equals("water")){
                    assertTrue(client.castSkill(3,enemy,0,0,0));waitFor(()->client.skills().cooldowns().get(2)>0,client,observer);capture(frames[0],evidence,"prisao-agua-polida");
                    assertTrue(client.castSkill(1,enemy,0,0,0));waitFor(()->client.skills().events().stream().anyMatch(e->e.skill().equals("water_1")&&e.fromX()==e.x()&&e.fromY()==e.y()),client,observer);capture(frames[1],evidence,"impacto-corte-agua");
                    assertTrue(client.castSkill(4,0,0,0,0));waitFor(()->client.skills().cooldowns().get(3)>0,client,observer);capture(frames[0],evidence,"vortex-agua-polido");
                }else{
                    assertTrue(client.castSkill(2,enemy,0,0,0));
                    waitFor(()->client.skills().events().stream().filter(e->e.skill().equals("wind_2")&&(e.fromX()!=e.x()||e.fromY()!=e.y())).count()==3,client,observer);
                    capture(frames[0],evidence,"vento-tres-lancamentos");
                }
                assertNull(server.failure());assertTrue(client.connected()&&observer.connected());
            }
        }finally{SwingUtilities.invokeAndWait(()->{for(var f:frames)if(f!=null)f.dispose();});if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);}
    }
    private static NetworkSkillPresentation presentation(JFrame frame){return (NetworkSkillPresentation)frame.getRootPane().getClientProperty("networkSkillPresentation");}
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static boolean ready(LocalClient c){return c.playerSheet()!=null&&c.skills()!=null&&!c.revisions().blocked();}
    private static void waitFor(Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(condition,clients);}
    private static void capture(JFrame frame,Path folder,String name)throws Exception{GamePanelsIntegrationTest.capture(frame,folder.resolve(name+".png"));}
}
