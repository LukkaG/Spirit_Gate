package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.WorldContent;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita o toggle real e registra os efeitos novos em dois clientes Swing/TCP. */
class SentryRefinementGuiTest {
    @TempDir Path temporary;
    @Test void keyboardCancelsSentryDuringCooldownAndMartialUsesTheNewEffects()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();
        var evidence=Files.createDirectories(root.resolve("docs/skills-preview-v3/runtime"));
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var world=new RpgSessionWorld(List.of("aetheri","human"),null,true,20);world.enableRevisions(WorldContent.baseline());
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");
        System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=connect(server,"Arqueiro Energia","aetheri");var b=connect(server,"Marcial Impacto","human")){
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(a,sprites,"Sentinela V3",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                    frames[1]=RpgNetworkClientLauncher.open(b,sprites,"Marcial V3",null,CosmeticCatalog.Selection.none(),f->{},visuals);
                });
                waitFor(()->ready(a)&&ready(b),a,b);
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"bow_long");b.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"martial");
                waitFor(()->a.playerSheet().progress().domain().equals("bow_long")&&b.playerSheet().progress().domain().equals("martial"),a,b);
                waitFor(()->GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frames[0],"hudDomainSkill2").isEnabled()),a,b);
                focusWorld(frames[0]);SwingKeys.tap(KeyEvent.VK_F6);
                waitFor(()->a.skills().sentryShots()>0,a,b);var before=GamePanelsIntegrationTest.self(a);
                SwingKeys.press(KeyEvent.VK_W);long tick=a.snapshot().tick();
                waitFor(()->a.snapshot().tick()>tick+4,a,b);SwingKeys.release(KeyEvent.VK_W);
                assertEquals(before.x(),GamePanelsIntegrationTest.self(a).x(),.01);assertEquals(before.y(),GamePanelsIntegrationTest.self(a).y(),.01);
                assertTrue(GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frames[0],"hudDomainSkill2").isEnabled()));
                GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("sentinela-ativa.png"));
                SwingKeys.tap(KeyEvent.VK_F6);waitFor(()->a.skills().sentryShots()==0,a,b);assertTrue(a.skills().cooldowns().get(1)>0);
                SwingKeys.tap(KeyEvent.VK_SPACE);waitFor(()->a.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_1")),a,b);
                GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("flecha-energia-centro.png"));
                focusWorld(frames[1]);SwingKeys.tap(KeyEvent.VK_F7);
                waitFor(()->b.skills().cooldowns().get(2)>0,a,b);GamePanelsIntegrationTest.capture(frames[1],evidence.resolve("impacto-sismico-ampliado.png"));
                SwingKeys.tap(KeyEvent.VK_F9);waitFor(()->b.skills().cooldowns().get(4)>0,a,b);
                GamePanelsIntegrationTest.capture(frames[1],evidence.resolve("combo-punhos-chutes.png"));assertNull(server.failure());
            }
        }finally{
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingKeys.clear();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }
    /** Hotkeys partem do mundo: o botão da técnica pode perder foco ao entrar em recarga. */
    private static void focusWorld(JFrame frame)throws Exception{
        var panel=(JComponent)frame.getRootPane().getClientProperty("networkPanel");
        SwingUtilities.invokeAndWait(()->{frame.toFront();panel.requestFocusInWindow();});
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        while(!GamePanelsIntegrationTest.onEdt(panel::isFocusOwner)&&System.nanoTime()<deadline)Thread.sleep(20);
        assertTrue(GamePanelsIntegrationTest.onEdt(panel::isFocusOwner),"O mundo precisa receber as hotkeys");
    }
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static boolean ready(LocalClient c){return c.playerSheet()!=null&&c.skills()!=null&&!c.revisions().blocked();}
    private static void waitFor(Task201CharacterSessionTest.Check condition,LocalClient... clients)throws Exception{Task201CharacterSessionTest.waitFor(condition,clients);}
}
