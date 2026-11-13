package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.prototype.rpg.CosmeticCatalog;
import com.projetok.studio.WorldContent;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.nio.file.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Teclado AWT e dois clientes reais: smartcast, área por teclado e seleção de aliados. */
@org.junit.jupiter.api.extension.ExtendWith(GuiInputTrace.class)
class SkillInputRefinementGuiTest {
    @TempDir Path temporary;
    @ParameterizedTest @ValueSource(booleans={true,false})
    void groundUsesKeyboardWithoutMovingPlayerAndConfirmsWithEnterOrSameSkill(boolean enter)throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var world=new RpgSessionWorld(java.util.List.of("aetheri","human"),null,true,20);world.enableRevisions(WorldContent.baseline());var frames=new JFrame[2];
        String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=connect(server,"Mira Aetheri","aetheri");var b=connect(server,"Alvo Aliado","human")){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"Mira TCP",null,CosmeticCatalog.Selection.none(),f->{},visuals);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"Aliado TCP",null,CosmeticCatalog.Selection.none(),f->{},visuals);});
                Task201CharacterSessionTest.waitFor(()->ready(a)&&ready(b),a,b);a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"bow_long");b.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"light");
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("bow_long")&&b.playerSheet().progress().domain().equals("light"),a,b);
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frames[0],"hudDomainSkill3").isEnabled()),a,b);
                GamePanelsIntegrationTest.focus(frames[0],"hudDomainSkill3");var initial=GamePanelsIntegrationTest.self(a);SwingKeys.press(KeyEvent.VK_UP);
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.self(a).y()<initial.y()-8,a,b);SwingKeys.release(KeyEvent.VK_UP);
                long settled=a.snapshot().tick();Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>settled+3,a,b);
                // A skill em linha deve sair para cima, que é o facing confirmado pelo movimento.
                SwingKeys.tap(KeyEvent.VK_F8);assertFalse(GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aiming()));
                Task201CharacterSessionTest.waitFor(()->a.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_4")),a,b);
                var shot=a.skills().events().stream().filter(e->e.skill().equals("aetheri_4")).findFirst().orElseThrow();assertEquals(shot.fromX(),shot.x(),.01);assertTrue(shot.y()<shot.fromY());
                SwingKeys.tap(KeyEvent.VK_F7);assertTrue(GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aiming()));SwingKeys.tap(KeyEvent.VK_ESCAPE);
                assertFalse(GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aiming()));assertEquals(0,a.skills().cooldowns().get(2));
                SwingKeys.tap(KeyEvent.VK_F7);var before=GamePanelsIntegrationTest.self(a);float aim=GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aimX());SwingKeys.press(KeyEvent.VK_RIGHT);
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aimX()>aim+12),a,b);
                assertEquals(before.x(),GamePanelsIntegrationTest.self(a).x(),.01);assertEquals(before.y(),GamePanelsIntegrationTest.self(a).y(),.01);
                SwingKeys.tap(enter?KeyEvent.VK_ENTER:KeyEvent.VK_F7);assertFalse(GamePanelsIntegrationTest.onEdt(()->controls(frames[0]).aiming()));
                Task201CharacterSessionTest.waitFor(()->a.skills().cooldowns().get(2)>0,a,b);
                long confirmed=a.snapshot().tick();SwingKeys.press(KeyEvent.VK_RIGHT);Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>confirmed+4,a,b);
                assertEquals(before.x(),GamePanelsIntegrationTest.self(a).x(),.01,"Tecla mantida não pode virar caminhada após confirmar");SwingKeys.release(KeyEvent.VK_RIGHT);
                // Clique no personagem usa a mesma seleção única que TAB e mantém ataques hostis bloqueados.
                SwingUtilities.invokeAndWait(()->{
                    var panel=(DebugPanel)frames[0].getRootPane().getClientProperty("networkPanel");var ally=b.snapshot().actors().stream().filter(v->v.id()==b.id()).findFirst().orElseThrow();
                    var base=panel.aimAt(300,300);var unit=panel.aimAt(301,301);int x=(int)Math.round(300+(ally.x()-base.x)/(unit.x-base.x));int y=(int)Math.round(300+(ally.y()-24-base.y)/(unit.y-base.y));
                    panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,MouseEvent.BUTTON1));
                });
                assertEquals(b.id(),a.selectedTarget());assertFalse(a.send(Protocol.Action.ATTACK,0,0));assertFalse(a.send(Protocol.Action.LIGHTNING_STEP,0,0));
                var evidence=Files.createDirectories(root.resolve(".task-checkpoints/SKILLS40/refinamento-v2/ui"));GamePanelsIntegrationTest.capture(frames[0],evidence.resolve(enter?"area-enter.png":"area-mesma-tecla.png"));assertNull(server.failure());
            }
        } finally {SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingKeys.clear();if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);}
    }
    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
    private static boolean ready(LocalClient client){return !client.revisions().blocked()&&client.playerSheet()!=null&&client.skills()!=null;}
    private static NetworkSkillControls controls(JFrame frame){return (NetworkSkillControls)frame.getRootPane().getClientProperty("networkSkillControls");}
}
