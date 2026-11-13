package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.studio.WorldContent;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Dois clientes TCP reais, teclado AWT e capturas com os PNGs adaptados no mundo. */
class NetworkSkillGuiTest {
    @TempDir Path temporary;
    @Test void fiveDomainSlotsUseRealInputAndBothClientsRenderTheConfirmedVortex()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        var root=Path.of("..").toAbsolutePath().normalize();var evidence=Files.createDirectories(root.resolve("docs/skills-preview/runtime"));
        var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
        var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);world.enableRevisions(WorldContent.baseline());
        var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();
            try(var a=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Skills Reaper","reaper","none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
                var b=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Skills Human","human","none","none"),false,false,null,Task201CharacterSessionTest.EMPTY)){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"TCP habilidades",null,null,f->{},visuals);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"TCP observador",null,null,f->{},visuals);});
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null&&a.skills()!=null&&b.skills()!=null,a,b);
                assertTrue(a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"water"));assertTrue(b.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"martial"));
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("water")&&b.playerSheet().progress().domain().equals("martial"),a,b);
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->view(frames[0]).clip("water_4")!=null&&view(frames[1]).clip("water_4")!=null),a,b);
                SwingUtilities.invokeAndWait(()->{
                    assertEquals(9,GamePanelsIntegrationTest.find(frames[0],"hudActionBlock") instanceof Container block?block.getComponentCount():0);
                    var router=(GameInputRouter)frames[0].getRootPane().getClientProperty("networkInputRouter");assertEquals(KeyEvent.VK_F8,router.shortcuts().get("domain-skill-4"));
                    assertNotNull(GamePanelsIntegrationTest.find(frames[0],"hudDomainSkill5"));assertNull(frames[0].getRootPane().getClientProperty("networkSkillAssetsError"));
                });
                // O cone usa o facing e sai ao pressionar a tecla, sem exigir mouse ou confirmação.
                GamePanelsIntegrationTest.focus(frames[1],"hudDomainSkill5");SwingKeys.tap(KeyEvent.VK_F9);
                assertFalse(GamePanelsIntegrationTest.onEdt(()->controls(frames[1]).aiming()));
                Task201CharacterSessionTest.waitFor(()->b.skills().events().stream().anyMatch(event->event.skill().equals("martial_5")),a,b);
                assertFalse(GamePanelsIntegrationTest.onEdt(()->((GamePanels)frames[1].getRootPane().getClientProperty("networkPanels")).anyOpen()));
                GamePanelsIntegrationTest.focus(frames[0],"hudDomainSkill4");SwingKeys.tap(KeyEvent.VK_F8);
                Task201CharacterSessionTest.waitFor(()->a.skills().events().stream().anyMatch(e->e.skill().equals("water_4"))&&b.skills().events().stream().anyMatch(e->e.skill().equals("water_4")),a,b);
                Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->view(frames[0]).activeCount()>0&&view(frames[1]).activeCount()>0),a,b);
                GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("cliente-water-vortex.png"));GamePanelsIntegrationTest.capture(frames[1],evidence.resolve("observador-water-vortex.png"));
                assertTrue(a.skills().cooldowns().get(3)>0);assertNull(server.failure());
                GamePanelsIntegrationTest.click(frames[0],"hudSkills");GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("habilidades-agua.png"));
                GamePanelsIntegrationTest.click(frames[0],"close-skills");
                // Os nove slots precisam continuar utilizáveis nas menores resoluções suportadas.
                for(var size:new int[][]{{1000,700},{640,480}}){
                    SwingUtilities.invokeAndWait(()->{
                        frames[0].setSize(size[0],size[1]);GamePanelsIntegrationTest.layout(frames[0]);
                        var block=(Container)GamePanelsIntegrationTest.find(frames[0],"hudActionBlock");
                        assertEquals(9,block.getComponentCount());
                        var content=frames[0].getContentPane();var contentBounds=new Rectangle(0,0,content.getWidth(),content.getHeight());
                        for(int index=0;index<block.getComponentCount();index++){
                            var slot=block.getComponent(index);assertTrue(slot.isVisible());assertTrue(slot.getWidth()>=24&&slot.getHeight()>=24);
                            assertTrue(new Rectangle(0,0,block.getWidth(),block.getHeight()).contains(slot.getBounds()),slot.getName());
                            assertTrue(contentBounds.contains(SwingUtilities.convertRectangle(block,slot.getBounds(),content)),slot.getName());
                            for(int other=index+1;other<block.getComponentCount();other++)assertFalse(slot.getBounds().intersects(block.getComponent(other).getBounds()),"Slots sobrepostos");
                        }
                        var worldPanel=(JComponent)frames[0].getRootPane().getClientProperty("networkPanel");
                        assertNotNull(worldPanel);assertEquals(contentBounds,worldPanel.getBounds());
                    });
                    GamePanelsIntegrationTest.capture(frames[0],evidence.resolve("hotbar-nove-slots-"+size[0]+".png"));
                }
            }
        } finally {
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingKeys.clear();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }
    private static NetworkSkillPresentation view(JFrame frame){return (NetworkSkillPresentation)frame.getRootPane().getClientProperty("networkSkillPresentation");}
    private static NetworkSkillControls controls(JFrame frame){return (NetworkSkillControls)frame.getRootPane().getClientProperty("networkSkillControls");}
}
