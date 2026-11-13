package com.projetok.multiplayer;

import com.projetok.studio.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task213HudSwingTest {
    @TempDir Path temporary;
    @Test void twoRealClientsKeepActionsPanelsMinimapAndResponsiveHudWorking()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var project=Path.of("..").toAbsolutePath();var evidence=Path.of(System.getProperty("spiritgate.test.evidenceRoot",project.resolve(".task-checkpoints/TASK-213/corujao-viii/ui").toString()));Files.createDirectories(evidence);
        var assets=AssetCatalog.scan(project,null);var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var sprites=RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5);
        var presentation=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)) {
            server.start();
            try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Akira Reaper","reaper","none","none"),false,false,null,assets);
                var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Mika Human","human","none","none"),false,false,null,assets)) {
                SwingUtilities.invokeAndWait(()->{
                    frames[0]=RpgNetworkClientLauncher.open(a,sprites,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                    frames[1]=RpgNetworkClientLauncher.open(b,sprites,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                });
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                Thread.sleep(250);
                SwingUtilities.invokeAndWait(()->{
                    try {
                        for(int i=0;i<2;i++) {
                            var frame=frames[i];assertInstanceOf(NetworkGameHud.class,frame.getContentPane());
                            for(var size:new int[][]{{1366,768},{1000,700},{640,480}}){
                                frame.setSize(size[0],size[1]);layout(frame);
                                for(String name:new String[]{"hudMinimap","hudProfile","hudInventory","hudJournal","hudMenu","hudSettings","hudSkill0","hudSkill3"}) {
                                    var component=find(frame,name);assertNotNull(component,name);var bounds=SwingUtilities.convertRectangle(component.getParent(),component.getBounds(),frame.getContentPane());
                                    assertTrue(frame.getContentPane().getBounds().contains(bounds),name+" "+bounds);
                                }
                                var left=find(frame,"hudLeftTools").getBounds();var center=find(frame,"hudActionBlock").getBounds();var right=find(frame,"hudRightTools").getBounds();
                                assertTrue(left.getMaxX()<center.x);assertTrue(center.getMaxX()<right.x);
                                assertEquals(left.y,center.y);assertEquals(center.y,right.y);assertEquals(left.height,center.height);
                                var resources=find(frame,"hudResources").getBounds();assertEquals(center.x,resources.x);assertEquals(center.width,resources.width);
                                int reserves=0;for(int slot=0;slot<4;slot++){var reserve=find(frame,"hudReserve"+slot);assertFalse(reserve instanceof AbstractButton);assertFalse(reserve.isFocusable());if(reserve.isVisible())reserves++;}
                                assertEquals(4,reserves);assertTrue(center.width<=320);assertTrue(find(frame,"hudSkill0").getWidth()<=36);assertTrue(right.getMaxX()-left.x<=560);
                                capture(frame,evidence.resolve("client-"+i+"-"+size[0]+".png"));
                            }
                            frame.setSize(1366,768);layout(frame);
                        }
                        var hud=(NetworkGameHud)frames[0].getContentPane();hud.toggleMinimap();assertFalse(find(frames[0],"hudMinimap").isVisible());hud.toggleMinimap();assertTrue(find(frames[0],"hudMinimap").isVisible());
                        String[] buttons={"hudProfile","hudInventory","hudJournal","hudMenu"},ids={"profile","inventory","journal","menu"};
                        for(int i=0;i<buttons.length;i++){
                            ((JButton)find(frames[0],buttons[i])).doClick();assertTrue(find(frames[0],"panel-"+ids[i]).isVisible());
                            ((JButton)find(frames[0],"close-"+ids[i])).doClick();assertFalse(find(frames[0],"panel-"+ids[i]).isVisible());
                        }
                        assertEquals(0,frames[0].getOwnedWindows().length);
                        ((JButton)find(frames[0],"hudSkill0")).doClick();
                    }catch(Exception error){throw new RuntimeException(error);}
                });
                a.await(s->a.combatEvents().stream().anyMatch(e->e.kind().equals("HIT")&&e.player()==a.id()),Duration.ofSeconds(4));
                float startY=a.snapshot().actors().stream().filter(actor->actor.id()==a.id()).findFirst().orElseThrow().y();
                var input=(JComponent)frames[0].getRootPane().getClientProperty("networkPanel");
                SwingUtilities.invokeAndWait(()->input.getActionMap().get("Strue").actionPerformed(new ActionEvent(input,0,"Strue")));
                a.await(s->s.actors().stream().anyMatch(actor->actor.id()==a.id()&&actor.y()>startY+8),Duration.ofSeconds(3));
                SwingUtilities.invokeAndWait(()->input.getActionMap().get("Sfalse").actionPerformed(new ActionEvent(input,0,"Sfalse")));
                b.await(s->s.actors().stream().anyMatch(actor->actor.id()==a.id()&&actor.y()>startY+8),Duration.ofSeconds(3));
                SwingUtilities.invokeAndWait(()->((JButton)find(frames[0],"hudSettings")).doClick());
                SwingUtilities.invokeAndWait(()->{assertTrue(find(frames[0],"panel-settings").isVisible());((JButton)find(frames[0],"settingsCancel")).doClick();assertFalse(find(frames[0],"panel-settings").isVisible());});
                assertFalse(Files.exists(temporary.resolve("saves/preferences.properties")),"Abrir/fechar opções não grava preferências");
                a.close();Thread.sleep(100);SwingUtilities.invokeAndWait(()->{for(int i=0;i<4;i++)assertFalse(find(frames[0],"hudSkill"+i).isEnabled());});assertNull(server.failure());
            }
        } finally {
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }
    private static Component find(Container root,String name){if(name.equals(root.getName()))return root;for(var child:root.getComponents()){if(name.equals(child.getName()))return child;if(child instanceof Container container){var found=find(container,name);if(found!=null)return found;}}return null;}
    private static void layout(Container root){root.doLayout();for(var child:root.getComponents())if(child instanceof Container container)layout(container);}
    private static void capture(JFrame frame,Path file)throws Exception{var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frame.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",file.toFile());}
}
