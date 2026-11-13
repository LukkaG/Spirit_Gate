package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.WorldContent;
import java.awt.GraphicsEnvironment;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Captura o ciclo V5 confirmado em dois clientes TCP, incluindo o chão da erupção. */
class SkillLifecycleGuiTest {
    @TempDir Path temporary;

    @ParameterizedTest @ValueSource(strings={"water_4","fire_3","fire_4"})
    void lifecycleRendersOnBothClientsAndExpiresWithoutReplaying(String skill)throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();
        var evidence=Files.createDirectories(root.resolve("docs/skills-preview-v5/runtime"));
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var world=new RpgSessionWorld(List.of("reaper","human"),null,true,20);
        world.enableRevisions(WorldContent.baseline());
        var frames=new JFrame[2];
        String previous=System.getProperty("spiritgate.rpg.dataDir");
        System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();
            try(var caster=connect(server,"Ciclo V5","reaper");var observer=connect(server,"Observador V5","human")){
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(caster,sprites,"Ciclo V5",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                    frames[1]=RpgNetworkClientLauncher.open(observer,sprites,"Observador V5",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                });
                waitFor(()->ready(caster)&&ready(observer),caster,observer);
                String domain=skill.startsWith("water")?"water":"fire";
                assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,domain));
                waitFor(()->caster.playerSheet().progress().domain().equals(domain),caster,observer);
                waitFor(()->GamePanelsIntegrationTest.onEdt(()->loaded(frames[0],skill)&&loaded(frames[1],skill)),caster,observer);
                assertTrue(caster.castSkill(Integer.parseInt(skill.substring(skill.length()-1)),0,0,0,0));
                // A erupção só começa no evento pontual recebido depois da viagem da fissura.
                waitFor(()->caster.skills().events().stream().anyMatch(e->e.skill().equals(skill)&&e.fromX()==e.x()&&e.fromY()==e.y()),caster,observer);
                long began=caster.skills().events().stream().filter(e->e.skill().equals(skill)&&e.fromX()==e.x()&&e.fromY()==e.y()).findFirst().orElseThrow().tick();
                String[] phases={"inicio","apice","fim"};
                int[] ticks=skill.equals("fire_4")?new int[]{2,16,33}:new int[]{2,18,36};
                for(int phase=0;phase<ticks.length;phase++){
                    long target=began+ticks[phase];
                    waitFor(()->caster.snapshot().tick()>=target&&observer.snapshot().tick()>=target,caster,observer);
                    GamePanelsIntegrationTest.capture(frames[0],evidence.resolve(skill+"-jogo-"+phases[phase]+".png"));
                    GamePanelsIntegrationTest.capture(frames[1],evidence.resolve(skill+"-observador-"+phases[phase]+".png"));
                }
                waitFor(()->caster.snapshot().tick()>began+60,caster,observer);
                assertEquals(0,GamePanelsIntegrationTest.onEdt(()->view(frames[0]).activeCount()));
                assertEquals(0,GamePanelsIntegrationTest.onEdt(()->view(frames[1]).activeCount()));
                assertNull(server.failure());assertTrue(caster.connected()&&observer.connected());
            }
        }finally{
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});
            SwingKeys.drain();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }

    private static boolean loaded(JFrame frame,String skill){var clip=view(frame).clip(skill);return clip!=null&&clip.frames()==12&&clip.columns()==4;}
    private static NetworkSkillPresentation view(JFrame frame){return (NetworkSkillPresentation)frame.getRootPane().getClientProperty("networkSkillPresentation");}
    private static boolean ready(LocalClient client){return client.playerSheet()!=null&&client.skills()!=null&&!client.revisions().blocked();}
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static void waitFor(Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(condition,clients);}
}
