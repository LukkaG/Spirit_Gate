package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TargetCombatGuiTest {
    @TempDir Path temporary;
    @Test void tabCyclesRealEnemiesAndTargetedBasicTurnsForBothClientsWithoutWalking()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath();var evidence=Path.of(System.getProperty("spiritgate.test.evidenceRoot",root.resolve(".task-checkpoints/TARGET-COMBATE/ui").toString()));Files.createDirectories(evidence);
        var assets=AssetCatalog.scan(root,null);var catalog=CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1"));var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visual=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);var frames=new JFrame[2];
        var old=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Target Reaper","reaper","none","none"),false,false,null,assets);
                var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Target Human","human","none","none"),false,false,null,assets)){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"TCP alvo",catalog,CosmeticCatalog.Selection.none(),f->{},visual);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"TCP alvo",catalog,CosmeticCatalog.Selection.none(),f->{},visual);});
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                GamePanelsIntegrationTest.focus(frames[0],"hudInventory");SwingKeys.tap(KeyEvent.VK_TAB);assertEquals(0,a.selectedTarget(),"TAB na HUD navega sem selecionar inimigo");
                SwingUtilities.invokeAndWait(()->((JComponent)frames[0].getRootPane().getClientProperty("networkPanel")).requestFocusInWindow());
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()==frames[0].getRootPane().getClientProperty("networkPanel")),a,b);
                SwingKeys.tap(KeyEvent.VK_TAB);int first=a.selectedTarget();assertTrue(first>1_000_000&&first<2_000_000);assertFalse(((GamePanels)frames[0].getRootPane().getClientProperty("networkPanels")).anyOpen());
                SwingKeys.tap(KeyEvent.VK_TAB);int second=a.selectedTarget();assertNotEquals(first,second);assertEquals(0,b.selectedTarget());
                var before=GamePanelsIntegrationTest.self(a);SwingKeys.tap(KeyEvent.VK_SPACE);
                Task201CharacterSessionTest.waitFor(()->{var target=a.snapshot().actors().stream().filter(v->v.id()==second).findFirst().orElse(null);var self=GamePanelsIntegrationTest.self(a);if(target==null)return false;double dx=target.x()-self.x(),dy=target.y()-self.y(),length=Math.hypot(dx,dy);return length>1&&(self.facingX()*dx+self.facingY()*dy)/length>.99;},a,b);
                var turned=GamePanelsIntegrationTest.self(a);assertEquals(before.x(),turned.x(),.01);assertEquals(before.y(),turned.y(),.01);
                Task201CharacterSessionTest.waitFor(()->b.snapshot().actors().stream().anyMatch(v->v.id()==a.id()&&Math.abs(v.facingX()-turned.facingX())<.001&&Math.abs(v.facingY()-turned.facingY())<.001),a,b);
                for(int width:new int[]{1366,1000,640}){SwingUtilities.invokeAndWait(()->{frames[0].setSize(width,width==1366?768:width==1000?700:480);GamePanelsIntegrationTest.layout(frames[0]);var panel=GamePanelsIntegrationTest.find(frames[0],"hudTarget");assertTrue(frames[0].getContentPane().getBounds().contains(panel.getBounds()));});GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("target-"+width+".png"));}
                GamePanelsIntegrationTest.click(frames[0],"hudClearTarget");assertEquals(0,a.selectedTarget());GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("target-cleared.png"));
                GamePanelsIntegrationTest.click(frames[0],"hudInventory");GamePanelsIntegrationTest.focus(frames[0],"inventorySearch");SwingKeys.tap(KeyEvent.VK_TAB);assertEquals(0,a.selectedTarget());SwingKeys.tap(KeyEvent.VK_ESCAPE);
                a.close();Task201CharacterSessionTest.waitFor(()->a.selectedTarget()==0,b,b);assertNull(server.failure());
            }
        }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});if(old==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",old);}
    }
}
