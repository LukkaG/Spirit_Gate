package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CompactHudCharacterGuiTest {
    @TempDir Path temporary;
    @Test void itemPopoverEquipmentSeparatedSkillsAndAttributeButtonsUseConfirmedState()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path root=Path.of("..").toAbsolutePath(),evidence=Path.of(System.getProperty("spiritgate.test.evidenceRoot",root.resolve(".task-checkpoints/HUD-COMPACTA/ui").toString()));Files.createDirectories(evidence);
        var assets=AssetCatalog.scan(root,null);var catalog=CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1"));var sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);var presentation=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true,20);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);var frames=new JFrame[2];String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Ficha Reaper","reaper","none","none"),false,false,null,assets);
                var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Ficha Human","human","none","none"),false,false,null,assets)){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"TCP ficha",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"TCP ficha",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);});Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                JFrame frame=frames[0];GamePanelsIntegrationTest.click(frame,"hudProfile");Task201CharacterSessionTest.waitFor(()->a.attributes()!=null&&b.attributes()!=null&&GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"allocate-strength").isEnabled()),a,b);
                assertEquals(71,a.attributes().points());int attack=a.playerSheet().progress().attack();
                GamePanelsIntegrationTest.click(frame,"allocate-strength");Task201CharacterSessionTest.waitFor(()->a.attributes().strength()==1&&GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"allocate-strength").isEnabled()),a,b);
                GamePanelsIntegrationTest.click(frame,"allocate-strength");Task201CharacterSessionTest.waitFor(()->a.attributes().strength()==2&&a.playerSheet().progress().attack()==attack+1,a,b);assertEquals(69,a.attributes().points());assertEquals(0,b.attributes().strength());assertEquals(71,b.attributes().points());
                for(String attribute:new String[]{"intelligence","agility"})for(int expected=1;expected<=2;expected++){final int value=expected;Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"allocate-"+attribute).isEnabled()),a,b);GamePanelsIntegrationTest.click(frame,"allocate-"+attribute);Task201CharacterSessionTest.waitFor(()->(attribute.equals("intelligence")?a.attributes().intelligence():a.attributes().agility())==value,a,b);}
                assertEquals(65,a.attributes().points());assertEquals(attack+1,a.playerSheet().progress().attack());SwingUtilities.invokeAndWait(()->{String effects=((JLabel)GamePanelsIntegrationTest.find(frame,"attributeCombatEffects")).getText();assertTrue(effects.contains("Domínio: +1"));assertTrue(effects.contains("2%"));assertTrue(effects.contains("1,0%"));});
                for(int expected=1;expected<=2;expected++){final int value=expected;Task201CharacterSessionTest.waitFor(()->a.energy()!=null&&GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"allocate-energy").isEnabled()),a,b);GamePanelsIntegrationTest.click(frame,"allocate-energy");Task201CharacterSessionTest.waitFor(()->a.energy()!=null&&a.energy().trained()==value&&a.attributes().points()==65-value&&GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"allocate-energy").isEnabled()),a,b);}
                assertEquals(1040,a.energy().maximum());assertEquals(1020,a.energy().regenerationPermille());assertEquals(63,a.attributes().points());SwingUtilities.invokeAndWait(()->assertTrue(((NetworkCombatHud)GamePanelsIntegrationTest.find(frame,"hudResources")).energyText().endsWith("/ 104")));
                SwingUtilities.invokeAndWait(()->{assertNull(GamePanelsIntegrationTest.find((Container)GamePanelsIntegrationTest.find(frame,"panel-profile"),"skillInfo0"));assertTrue(((JLabel)GamePanelsIntegrationTest.find(frame,"equipment-weapon")).getText().contains("Espada"));});
                for(int width:new int[]{1366,1000,640}){resize(frame,width);SwingUtilities.invokeAndWait(()->{var effects=(JComponent)GamePanelsIntegrationTest.find(frame,"attributeCombatEffects");effects.scrollRectToVisible(new Rectangle(0,0,effects.getWidth(),effects.getHeight()));});GamePanelsIntegrationTest.capture(frame,evidence.resolve("character-"+width+".png"));}
                GamePanelsIntegrationTest.click(frame,"unequip-weapon");Task201CharacterSessionTest.waitFor(()->!a.playerSheet().equipment().containsKey("weapon"),a,b);
                GamePanelsIntegrationTest.click(frame,"hudInventory");SwingUtilities.invokeAndWait(()->((JTextField)GamePanelsIntegrationTest.find(frame,"inventorySearch")).setText("Espada"));Task201CharacterSessionTest.waitFor(()->GamePanelsIntegrationTest.onEdt(()->((JList<?>)GamePanelsIntegrationTest.find(frame,"inventoryGrid")).getModel().getSize()==1),a,b);
                assertFalse(GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"inventoryItemPopup").isVisible()));GamePanelsIntegrationTest.focus(frame,"inventoryGrid");SwingKeys.tap(KeyEvent.VK_ENTER);assertTrue(GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"inventoryItemPopup").isVisible()));
                SwingKeys.tap(KeyEvent.VK_ESCAPE);assertFalse(GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"inventoryItemPopup").isVisible()));assertTrue(GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"panel-inventory").isVisible()));
                SwingUtilities.invokeAndWait(()->{var list=(JList<?>)GamePanelsIntegrationTest.find(frame,"inventoryGrid");list.dispatchEvent(new MouseEvent(list,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,20,20,1,false));});
                for(int width:new int[]{1366,1000,640}){resize(frame,width);SwingUtilities.invokeAndWait(()->{var popup=GamePanelsIntegrationTest.find(frame,"inventoryItemPopup");assertTrue(popup.isVisible());assertTrue(new Rectangle(0,0,popup.getParent().getWidth(),popup.getParent().getHeight()).contains(popup.getBounds()));});GamePanelsIntegrationTest.capture(frame,evidence.resolve("item-details-"+width+".png"));}
                GamePanelsIntegrationTest.click(frame,"inventoryEquip");Task201CharacterSessionTest.waitFor(()->"small_sword".equals(a.playerSheet().equipment().get("weapon"))&&!GamePanelsIntegrationTest.onEdt(()->GamePanelsIntegrationTest.find(frame,"inventoryItemPopup").isVisible()),a,b);
                GamePanelsIntegrationTest.click(frame,"hudSkills");SwingUtilities.invokeAndWait(()->{var skills=(Container)GamePanelsIntegrationTest.find(frame,"panel-skills");assertTrue(skills.isVisible());assertNotNull(GamePanelsIntegrationTest.find(skills,"skillInfo0"));assertNull(GamePanelsIntegrationTest.find(skills,"equipment-weapon"));});
                for(int width:new int[]{1366,1000,640}){resize(frame,width);GamePanelsIntegrationTest.capture(frame,evidence.resolve("skills-"+width+".png"));}
                GamePanelsIntegrationTest.click(frame,"close-skills");resize(frame,1366);GamePanelsIntegrationTest.capture(frame,evidence.resolve("hud-compact-1366.png"));resize(frame,640);GamePanelsIntegrationTest.capture(frame,evidence.resolve("hud-compact-640.png"));assertEquals(0,frame.getOwnedWindows().length);assertNull(server.failure());
            }
        }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);}
    }
    private static void resize(JFrame frame,int width)throws Exception{SwingUtilities.invokeAndWait(()->{frame.setSize(width,width==1366?768:width==1000?700:480);GamePanelsIntegrationTest.layout(frame);});}
}
