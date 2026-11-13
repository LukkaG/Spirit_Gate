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

/** Dois clientes TCP conferem as novas folhas e o combo frontal de vento com retorno target. */
class SkillSlashRefinementGuiTest {
    @TempDir Path temporary;

    @ParameterizedTest @ValueSource(strings={"fire","water","martial","wind"})
    void slashesRenderOnCasterAndObserverAndWindRequiresActualContact(String kit)throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();var evidence=Files.createDirectories(root.resolve("docs/skills-preview-v6/runtime"));
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
        maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),300,300):e).toList()));
        String race=kit.equals("martial")?"human":"reaper";
        var world=new RpgSessionWorld(List.of(race,"human"),null,true,20);
        world.enableRevisions(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY));
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var caster=connect(server,"Cortes V6",race);var observer=connect(server,"Observador V6","human")){
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(caster,sprites,"Cortes V6",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                    frames[1]=RpgNetworkClientLauncher.open(observer,sprites,"Observador V6",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                });
                waitFor(()->ready(caster)&&ready(observer),caster,observer);assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,kit));
                waitFor(()->caster.playerSheet().progress().domain().equals(kit),caster,observer);
                waitFor(()->GamePanelsIntegrationTest.onEdt(()->view(frames[0]).clip("fire_5")!=null&&view(frames[0]).clip("fire_5").frames()==12&&view(frames[1]).clip("wind_4.impact")!=null),caster,observer);
                if(kit.equals("wind")){
                    assertFalse(caster.skills().windCheckpoint());assertTrue(caster.castSkill(4,0,0,0,0));
                    waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals("wind_4")),caster,observer);
                    long start=last(caster,"wind_4");waitFor(()->caster.snapshot().tick()>=start+3,caster,observer);capture(frames[0],evidence,"vento-ida");
                    waitFor(()->caster.skills().windCheckpoint(),caster,observer);capture(frames[1],evidence,"vento-contato");
                    assertEquals(0,caster.selectedTarget());
                    assertTrue(caster.castSkill(5,0,0,0,0));waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals("wind_5")),caster,observer);
                    assertFalse(caster.skills().windCheckpoint());final long returning=last(caster,"wind_5");
                    waitFor(()->caster.snapshot().tick()>=returning+5,caster,observer);capture(frames[0],evidence,"vento-retorno");
                    assertEquals(1,caster.skills().events().stream().filter(e->e.skill().equals("wind_5")).count());
                }else{
                    int slot=kit.equals("fire")?5:kit.equals("water")?2:3;String id=kit+"_"+slot;
                    assertTrue(caster.castSkill(slot,0,0,250,300));waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals(id)),caster,observer);
                    long start=last(caster,id);int offset=kit.equals("fire")?5:kit.equals("water")?9:12;
                    waitFor(()->caster.snapshot().tick()>=start+offset,caster,observer);capture(frames[0],evidence,kit+"-apice");capture(frames[1],evidence,kit+"-observador");
                }
                assertNull(server.failure());assertTrue(caster.connected()&&observer.connected());
            }
        }finally{
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingKeys.drain();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }

    private static long last(LocalClient c,String skill){return c.skills().events().stream().filter(e->e.skill().equals(skill)).mapToLong(SkillProtocol.Event::tick).max().orElseThrow();}
    private static NetworkSkillPresentation view(JFrame frame){return (NetworkSkillPresentation)frame.getRootPane().getClientProperty("networkSkillPresentation");}
    private static boolean ready(LocalClient c){return c.playerSheet()!=null&&c.skills()!=null&&!c.revisions().blocked();}
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static void waitFor(Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(condition,clients);}
    private static void capture(JFrame frame,Path folder,String name)throws Exception{GamePanelsIntegrationTest.capture(frame,folder.resolve(name+".png"));}
}
