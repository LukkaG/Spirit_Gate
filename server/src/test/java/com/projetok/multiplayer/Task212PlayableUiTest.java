package com.projetok.multiplayer;

import com.projetok.studio.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.*;

class Task212PlayableUiTest {
    @Test void actualTwoClientWindowsExposeGameplayAndInventoryAndProduceCaptures()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var project=Path.of("..").toAbsolutePath();var assets=AssetCatalog.scan(project,null);
        var human=RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5);
        var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var presentation=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"),human),catalog.onLogicalCanvas(64,64,32,60,.4));
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);
        var frames=new JFrame[2];
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)) {
            server.start();
            try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Janela Reaper","reaper","none","none"),false,false,null,assets);
                var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Janela Human","human","none","none"),false,false,null,assets)) {
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(a,human,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                    frames[1]=RpgNetworkClientLauncher.open(b,human,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                });
                Task201CharacterSessionTest.ready(a,b);
                Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                Thread.sleep(300);
                var evidence=Path.of(System.getProperty("spiritgate.test.evidenceRoot",project.resolve(".task-checkpoints/TASK-212/corujao-vii/ui").toString()));Files.createDirectories(evidence);
                SwingUtilities.invokeAndWait(()->{
                    try {
                        for(int i=0;i<frames.length;i++) {
                            var frame=frames[i];assertTrue(frame.isShowing());
                            assertTrue(contains(frame,NetworkHotbar.class));assertTrue(contains(frame,NetworkCombatHud.class));
                            if(frame.getRootPane().getClientProperty("networkPanels") instanceof GamePanels integrated){
                                var input=(JComponent)frame.getRootPane().getClientProperty("networkPanel");input.getActionMap().get("hud-J").actionPerformed(new ActionEvent(input,0,"hud-J"));
                                assertNotNull(integrated.active());assertTrue(contains(integrated.active(),NetworkQuestPanel.class));integrated.escape();
                            }else assertTrue(contains(frame,NetworkQuestPanel.class));
                            var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frame.printAll(g);}finally{g.dispose();}
                            ImageIO.write(image,"png",evidence.resolve("client-"+i+".png").toFile());
                        }
                        var panel=(JComponent)frames[0].getRootPane().getClientProperty("networkPanel");
                        panel.getActionMap().get("inventory").actionPerformed(new ActionEvent(panel,0,"inventory"));
                        var integrated=(GamePanels)frames[0].getRootPane().getClientProperty("networkPanels");assertNotNull(integrated.active());
                        assertTrue(contains(integrated.active(),NetworkInventoryPanel.class));assertEquals(0,frames[0].getOwnedWindows().length);
                        var image=new BufferedImage(frames[0].getWidth(),frames[0].getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frames[0].printAll(g);}finally{g.dispose();}
                        ImageIO.write(image,"png",evidence.resolve("inventory.png").toFile());integrated.escape();
                    }catch(Exception e){throw new RuntimeException(e);}
                });
                assertNull(server.failure());
            }
        } finally {SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});}
    }
    private static boolean contains(Container root,Class<?> type) {
        for(var component:root.getComponents())if(type.isInstance(component)||component instanceof Container child&&contains(child,type))return true;
        return false;
    }
}
