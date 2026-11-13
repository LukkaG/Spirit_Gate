package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.rpg.*;
import com.projetok.rpg.RpgPreferences;
import com.projetok.studio.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GamePanelsIntegrationTest {
    @TempDir Path temporary;
    @Test void integratedPanelsRealKeysSettingsAndTwoClientsPreserveAuthority()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path project=Path.of("..").toAbsolutePath(),evidence=Path.of(System.getProperty("spiritgate.test.evidenceRoot",project.resolve(".task-checkpoints/ADENDO-JANELAS-HOTKEYS/corujao-viii/ui").toString()));Files.createDirectories(evidence);
        Path preferences=temporary.resolve("saves/preferences.properties");new RpgPreferences(33,true,false).save(preferences);byte[] original=Files.readAllBytes(preferences);
        String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        var frames=new JFrame[2];var routers=new GameInputRouter[2];
        var assets=AssetCatalog.scan(project,null);var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));var sprites=RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5);
        var visual=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();
            try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Akira Reaper","reaper","none","none"),false,false,null,assets);
                var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Mika Human","human","none","none"),false,false,null,assets)){
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},visual);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"TCP local",catalog,CosmeticCatalog.Selection.none(),f->{},visual);
                    for(int i=0;i<2;i++)routers[i]=(GameInputRouter)frames[i].getRootPane().getClientProperty("networkInputRouter");});
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                JFrame frame=frames[0];var panels=(GamePanels)frame.getRootPane().getClientProperty("networkPanels");
                int potions=Task207InventoryTest.count(a.playerSheet(),"potion");
                for(String focus:new String[]{"hudInventory","hudSettings","hudSkill3"}){
                    focus(frame,focus);long last=a.combatEvents().stream().mapToLong(Protocol.CombatEvent::sequence).max().orElse(0);SwingKeys.tap(KeyEvent.VK_SPACE);
                    Task201CharacterSessionTest.waitFor(()->a.combatEvents().stream().anyMatch(e->e.sequence()>last&&e.player()==a.id()&&e.kind().equals("HIT")),a,b);
                    Task201CharacterSessionTest.waitFor(()->b.combatEvents().stream().anyMatch(e->e.sequence()>last&&e.player()==a.id()&&e.kind().equals("HIT")),a,b);
                    assertFalse(panels.anyOpen());Thread.sleep(420);
                }
                assertEquals(potions,Task207InventoryTest.count(a.playerSheet(),"potion"));
                focus(frame,"hudInventory");SwingKeys.tap(KeyEvent.VK_ENTER);assertTrue(onEdt(()->find(frame,"panel-inventory").isVisible()));
                SwingKeys.tap(KeyEvent.VK_I);assertFalse(onEdt(()->find(frame,"panel-inventory").isVisible()));
                click(frame,"hudInventory");focus(frame,"inventorySearch");long hits=a.combatEvents().stream().filter(e->e.kind().equals("HIT")).count();float y=self(a).y();
                SwingKeys.tap(KeyEvent.VK_W);SwingKeys.tap(KeyEvent.VK_SPACE);SwingKeys.tap(KeyEvent.VK_E);
                assertEquals("w e",onEdt(()->((JTextField)find(frame,"inventorySearch")).getText()));assertEquals(hits,a.combatEvents().stream().filter(e->e.kind().equals("HIT")).count());assertEquals(y,self(a).y());
                capture(frame,evidence.resolve("inventory-text-focus.png"));SwingKeys.tap(KeyEvent.VK_ESCAPE);assertFalse(panels.anyOpen());
                SwingKeys.press(KeyEvent.VK_S);Task201CharacterSessionTest.waitFor(()->self(a).y()>y+8,a,b);SwingKeys.release(KeyEvent.VK_S);SwingKeys.drain();
                Task201CharacterSessionTest.waitFor(()->b.snapshot().actors().stream().anyMatch(actor->actor.id()==a.id()&&actor.y()>y+8),a,b);
                SwingUtilities.invokeAndWait(()->((JTextField)find(frame,"inventorySearch")).setText(""));
                for(var size:new int[][]{{1366,768},{1000,700},{640,480}}){
                    SwingUtilities.invokeAndWait(()->{frame.setSize(size[0],size[1]);layout(frame);});
                    for(var pair:new String[][]{{"hudInventory","inventory"},{"hudJournal","journal"},{"hudProfile","profile"},{"hudMenu","menu"},{"hudSettings","settings"}}){
                        click(frame,pair[0]);SwingUtilities.invokeAndWait(()->{layout(frame);var panel=(GamePanels.Panel)find(frame,"panel-"+pair[1]);assertTrue(panel.isVisible());assertTrue(frame.getContentPane().getBounds().contains(panel.getBounds()));
                            var alpha=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=alpha.createGraphics();try{panel.paintComponent(g);}finally{g.dispose();}int opacity=alpha.getRGB(panel.getWidth()/2,panel.getHeight()/2)>>>24;assertTrue(opacity>0&&opacity<255,"Navy precisa ter alfa real");
                            if(pair[1].equals("settings"))for(String name:new String[]{"settingsApply","settingsCancel","close-settings"}){var c=find(frame,name);var bounds=SwingUtilities.convertRectangle(c.getParent(),c.getBounds(),frame.getContentPane());assertTrue(panel.getBounds().contains(bounds),name);}
                        });
                        capture(frame,evidence.resolve(pair[1]+"-"+size[0]+".png"));click(frame,"close-"+pair[1]);assertEquals(0,frame.getOwnedWindows().length);
                    }
                }
                assertArrayEquals(original,Files.readAllBytes(preferences));
                click(frame,"hudSettings");Task201CharacterSessionTest.waitFor(()->onEdt(()->find(frame,"settingsApply").isEnabled()),a,b);
                SwingUtilities.invokeAndWait(()->{assertEquals(33,((JSlider)find(frame,"settingsVolume")).getValue());((JSlider)find(frame,"settingsVolume")).setValue(75);});
                focus(frame,"bind-SPACE");SwingKeys.tap(KeyEvent.VK_ENTER);SwingKeys.tap(KeyEvent.VK_SPACE);assertFalse(onEdt(()->((JButton)find(frame,"bind-SPACE")).getText().contains("Aguardando")));assertTrue(panels.modal());
                focus(frame,"bind-SPACE");SwingKeys.tap(KeyEvent.VK_ENTER);SwingKeys.tap(KeyEvent.VK_T);capture(frame,evidence.resolve("settings-rebind-focus.png"));click(frame,"settingsCancel");
                assertArrayEquals(original,Files.readAllBytes(preferences));assertEquals(KeyEvent.VK_SPACE,routers[0].shortcuts().get("SPACE"));
                click(frame,"hudSettings");Task201CharacterSessionTest.waitFor(()->onEdt(()->find(frame,"settingsApply").isEnabled()),a,b);
                SwingUtilities.invokeAndWait(()->{assertEquals(33,((JSlider)find(frame,"settingsVolume")).getValue());((JSlider)find(frame,"settingsVolume")).setValue(58);((JCheckBox)find(frame,"settingsMuted")).setSelected(false);});
                focus(frame,"bind-SPACE");SwingKeys.tap(KeyEvent.VK_ENTER);SwingKeys.tap(KeyEvent.VK_T);click(frame,"settingsApply");
                Task201CharacterSessionTest.waitFor(()->!panels.anyOpen(),a,b);assertEquals(58,RpgPreferences.load(preferences).volume());assertFalse(RpgPreferences.load(preferences).muted());assertEquals(KeyEvent.VK_T,routers[0].shortcuts().get("SPACE"));
                focus(frame,"hudInventory");SwingKeys.tap(KeyEvent.VK_T);assertFalse(panels.anyOpen());
                click(frame,"hudInventory");SwingUtilities.invokeAndWait(()->((JTextField)find(frame,"inventorySearch")).setText(""));
                capture(frame,evidence.resolve("inventory-bright.png"));SwingUtilities.invokeAndWait(()->((DebugPanel)frame.getRootPane().getClientProperty("networkPanel")).setAmbientLight(.5f));capture(frame,evidence.resolve("inventory-dark.png"));
                SwingUtilities.invokeAndWait(()->{
                    var panel=(GamePanels.Panel)find(frame,"panel-inventory");var header=(JComponent)panel.getComponent(0);header.dispatchEvent(new MouseEvent(header,MouseEvent.MOUSE_PRESSED,0,0,15,10,1,false));header.dispatchEvent(new MouseEvent(header,MouseEvent.MOUSE_DRAGGED,0,0,-3000,-3000,0,false));assertTrue(panel.getX()>=8&&panel.getY()>=8);
                    header.dispatchEvent(new MouseEvent(header,MouseEvent.MOUSE_PRESSED,0,0,15,10,1,false));header.dispatchEvent(new MouseEvent(header,MouseEvent.MOUSE_DRAGGED,0,0,3000,3000,0,false));assertTrue(panel.getX()+panel.getWidth()<=frame.getContentPane().getWidth()-8);assertTrue(panel.getY()+panel.getHeight()<=frame.getContentPane().getHeight()-8);
                    panels.escape();int children=frame.getContentPane().getComponentCount();for(int i=0;i<10;i++){((JButton)find(frame,"hudInventory")).doClick(0);((JButton)find(frame,"close-inventory")).doClick(0);}assertEquals(children,frame.getContentPane().getComponentCount());
                });assertNull(server.failure());
            }
        }finally{
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});SwingUtilities.invokeAndWait(()->{});
            for(var router:routers)if(router!=null)assertTrue(router.isClosed());
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }
    static Protocol.Actor self(LocalClient c){return c.snapshot().actors().stream().filter(a->a.id()==c.id()).findFirst().orElseThrow();}
    static Component find(Container root,String name){if(name.equals(root.getName()))return root;for(var c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container nested){var result=find(nested,name);if(result!=null)return result;}}return null;}
    static <T>T onEdt(java.util.concurrent.Callable<T> query)throws Exception{var result=new AtomicReference<T>();SwingUtilities.invokeAndWait(()->{try{result.set(query.call());}catch(Exception error){throw new RuntimeException(error);}});return result.get();}
    static void click(JFrame frame,String name)throws Exception{SwingUtilities.invokeAndWait(()->((JButton)find(frame,name)).doClick(0));Thread.sleep(60);}
    /** Aguarda a ativação nativa da janela antes de exigir foco no controle real. */
    static void focus(JFrame frame,String name)throws Exception{SwingUtilities.invokeAndWait(frame::toFront);
        long deadline=System.nanoTime()+Duration.ofSeconds(3).toNanos();
        while(!onEdt(()->KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()==find(frame,name))&&System.nanoTime()<deadline){
            SwingUtilities.invokeAndWait(()->{var c=(JComponent)find(frame,name);c.scrollRectToVisible(new Rectangle(0,0,c.getWidth(),c.getHeight()));GuiInputTrace.focusRequest(c,c.requestFocusInWindow());});Thread.sleep(20);
        }
        assertTrue(onEdt(()->KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()==find(frame,name)),name);}

    static void layout(Container root){root.doLayout();for(var c:root.getComponents())if(c instanceof Container nested)layout(nested);}
    static void capture(JFrame frame,Path path)throws Exception{SwingUtilities.invokeAndWait(()->{try{layout(frame);var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frame.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());}catch(Exception error){throw new RuntimeException(error);}});}
}
