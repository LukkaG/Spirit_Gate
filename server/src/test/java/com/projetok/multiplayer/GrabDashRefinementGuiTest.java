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

/** Captura pegada/pancada e rastros sobre o movimento real em duas janelas TCP. */
class GrabDashRefinementGuiTest {
    @TempDir Path temporary;

    @ParameterizedTest @ValueSource(strings={"martial_4","water_5","wind_3","thunder_4","thunder_5"})
    void grabAndDashPhasesStayAttachedToTheConfirmedWorld(String skill)throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();var evidence=Files.createDirectories(root.resolve("docs/skills-preview-v9/runtime"));
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        String kit=skill.substring(0,skill.indexOf('_')),race=kit.equals("martial")?"human":"reaper";
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");int target=kit.equals("martial")?185:300;
        maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),target,300):e).toList()));
        var world=new RpgSessionWorld(List.of(race,"human"),null,true,20);world.enableRevisions(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY));
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var caster=connect(server,"Movimento V9",race);var observer=connect(server,"Observador V9","human")){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(caster,sprites,"Movimento V9",null,CosmeticCatalog.Selection.none(),f->{},visuals);frames[1]=RpgNetworkClientLauncher.open(observer,sprites,"Observador V9",null,CosmeticCatalog.Selection.none(),f->{},visuals);});
                waitFor(()->ready(caster)&&ready(observer),caster,observer);assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,kit));
                waitFor(()->caster.playerSheet().progress().domain().equals(kit),caster,observer);
                waitFor(()->GamePanelsIntegrationTest.onEdt(()->view(frames[0]).clip("martial_4.impact")!=null&&view(frames[1]).clip("thunder_5.travel")!=null),caster,observer);
                int enemy=caster.snapshot().actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000).findFirst().orElseThrow().id();assertTrue(caster.selectTarget(enemy));
                var before=GamePanelsIntegrationTest.self(caster);int slot=Integer.parseInt(skill.substring(skill.length()-1));assertTrue(caster.castSkill(slot,skill.equals("thunder_5")?0:enemy,0,0,0));
                waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals(skill)),caster,observer);
                long start=caster.skills().events().stream().filter(e->e.skill().equals(skill)).mapToLong(SkillProtocol.Event::tick).min().orElseThrow();
                if(kit.equals("martial")){
                    waitFor(()->caster.snapshot().tick()>=start+2,caster,observer);capture(frames[0],evidence,"agarrão");
                    waitFor(()->caster.snapshot().tick()>=start+8,caster,observer);capture(frames[0],evidence,"body-slam");capture(frames[1],evidence,"body-slam-observador");
                    assertEquals(2,caster.skills().events().stream().filter(e->e.skill().equals(skill)).count());
                }else if(skill.equals("thunder_5")){
                    waitFor(()->GamePanelsIntegrationTest.onEdt(()->view(frames[0]).hidden(caster.id())&&view(frames[1]).hidden(caster.id())),caster,observer);
                    capture(frames[0],evidence,"raio-invisivel");capture(frames[1],evidence,"raio-invisivel-observador");
                    assertEquals(before.x(),GamePanelsIntegrationTest.self(caster).x(),.01);
                    waitFor(()->caster.snapshot().tick()>=start+34,caster,observer);
                    waitFor(()->GamePanelsIntegrationTest.onEdt(()->!view(frames[0]).hidden(caster.id())&&!view(frames[1]).hidden(caster.id())),caster,observer);
                    capture(frames[0],evidence,"raio-quatro-cortes");capture(frames[1],evidence,"raio-quatro-cortes-observador");
                }else{
                    waitFor(()->caster.snapshot().tick()>=start+5,caster,observer);capture(frames[0],evidence,skill+"-rastro");
                    assertTrue(Math.hypot(GamePanelsIntegrationTest.self(caster).x()-before.x(),GamePanelsIntegrationTest.self(caster).y()-before.y())>0);
                    waitFor(()->caster.snapshot().tick()>=start+6,caster,observer);capture(frames[1],evidence,skill+"-observador");

                }
                waitFor(()->caster.snapshot().tick()>start+(skill.equals("thunder_5")?55:30),caster,observer);
                assertEquals(0,GamePanelsIntegrationTest.onEdt(()->view(frames[0]).activeCount()));assertEquals(0,GamePanelsIntegrationTest.onEdt(()->view(frames[1]).activeCount()));
                assertNull(server.failure());assertTrue(caster.connected()&&observer.connected());
            }
        }finally{
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingKeys.drain();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }

    private static NetworkSkillPresentation view(JFrame frame){return (NetworkSkillPresentation)frame.getRootPane().getClientProperty("networkSkillPresentation");}
    private static boolean ready(LocalClient c){return c.playerSheet()!=null&&c.skills()!=null&&!c.revisions().blocked();}
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static void waitFor(Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(condition,clients);}
    private static void capture(JFrame frame,Path folder,String name)throws Exception{GamePanelsIntegrationTest.capture(frame,folder.resolve(name+".png"));}
}
