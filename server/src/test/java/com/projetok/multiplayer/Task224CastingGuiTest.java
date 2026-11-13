package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
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

/** Dois clientes TCP e eventos AWT: seleção, prévia de área, estados e desconexão da preparação. */
@org.junit.jupiter.api.extension.ExtendWith(GuiInputTrace.class)
class Task224CastingGuiTest {
    @TempDir Path temporary;
    @Test void viewportAndHudTabStaySeparateAndGroundPreviewNeverPretendsAcceptance()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture("bow_long")){
            f.focusWorld();assertEquals(0,f.caster.selectedTarget());
            SwingKeys.tap(KeyEvent.VK_F5);f.waitFor(()->edt(()->f.skills().feedbackText().contains("inimigo vivo")));
            f.capture("sem-alvo");
            SwingKeys.tap(KeyEvent.VK_TAB);int enemy=f.caster.selectedTarget();assertTrue(enemy>1_000_000&&enemy<2_000_000);
            GamePanelsIntegrationTest.focus(f.frames[0],"hudProfile");SwingKeys.tap(KeyEvent.VK_TAB);
            assertEquals(enemy,f.caster.selectedTarget());
            assertNotEquals(GamePanelsIntegrationTest.find(f.frames[0],"hudProfile"),edt(()->KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()));
            f.capture("alvo-inimigo");
            assertTrue(f.caster.selectTarget(f.observer.id()));f.waitFor(()->edt(()->!f.skills().buttons()[0].isEnabled()));
            assertTrue(edt(()->f.skills().buttons()[0].getToolTipText().contains("inimigo vivo")));f.capture("alvo-aliado-ofensiva-bloqueada");
            f.focusWorld();SwingKeys.tap(KeyEvent.VK_F7);assertTrue(edt(()->f.skills().aiming()));
            f.pointer(420,250);f.waitFor(()->edt(()->f.skills().feedbackText().contains("obstruída")));f.capture("area-obstruida-previa");
            SwingKeys.tap(KeyEvent.VK_ENTER);
            f.waitFor(()->edt(()->f.skills().feedbackText().contains("resultado desconhecido")));
            assertEquals(0,f.caster.skills().cooldowns().get(2));
            assertTrue(f.observer.skills().events().stream().noneMatch(e->e.skill().equals("aetheri_3")));
            f.capture("envio-sem-confirmacao-nao-e-rejeicao");
            f.focusWorld();SwingKeys.tap(KeyEvent.VK_F7);
            SwingUtilities.invokeAndWait(()->f.skills().pointer(10000,-10000));
            assertTrue(edt(()->f.skills().aimX()>=18&&f.skills().aimX()<=942&&f.skills().aimY()>=18&&f.skills().aimY()<=622));
            assertTrue(edt(()->Math.hypot(f.skills().aimX()-self(f.caster).x(),f.skills().aimY()-self(f.caster).y())<=300.1));
            SwingKeys.tap(KeyEvent.VK_ESCAPE);assertFalse(edt(()->f.skills().aiming()));assertEquals(0,f.caster.skills().cooldowns().get(2));
            f.waitFor(()->edt(()->f.skills().feedbackText().contains("cancelada")));f.capture("area-cancelada-sem-gasto");
            f.focusWorld();SwingKeys.tap(KeyEvent.VK_F7);f.pointer(250,400);f.capture("area-preparada");SwingKeys.tap(KeyEvent.VK_ENTER);
            f.waitFor(()->f.caster.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_3"))
                    &&f.observer.skills().events().stream().anyMatch(e->e.skill().equals("aetheri_3")));
            f.waitFor(()->edt(()->f.skills().feedbackText().contains("servidor")));
            assertFalse(edt(()->f.skills().feedbackText().contains("aceita")));
            f.capture("evento-servidor-recarga");f.captureObserver("evento-observador");
            assertTrue(f.caster.skills().cooldowns().get(2)>0);assertNull(f.server.failure());
            for(var size:new int[][]{{1000,700},{640,480}}){
                SwingUtilities.invokeAndWait(()->{f.frames[0].setSize(size[0],size[1]);GamePanelsIntegrationTest.layout(f.frames[0]);
                    var block=(Container)GamePanelsIntegrationTest.find(f.frames[0],"hudActionBlock");assertEquals(9,block.getComponentCount());
                    var feedback=GamePanelsIntegrationTest.find(f.frames[0],"hudSkillFeedback");assertTrue(feedback.getHeight()>=40);var content=f.frames[0].getContentPane();assertFalse(SwingUtilities.convertRectangle(block.getParent(),block.getBounds(),content).intersects(SwingUtilities.convertRectangle(feedback.getParent(),feedback.getBounds(),content)),"Faixa não pode ocupar a hotbar");});
                f.capture("hud-"+size[0]);
            }
        }
        // Marcadores reais da cena exercitam os avisos de alvo, sem fabricar distância no cliente.
        try(var f=new Fixture("bow_long",700,300,140,300)){
            int enemy=f.caster.snapshot().actors().stream().filter(NetworkSkillPreview::mob).findFirst().orElseThrow().id();
            assertTrue(f.caster.selectTarget(enemy));
            f.waitFor(()->edt(()->f.skills().buttons()[0].getToolTipText().contains("Fora de alcance")));
            GamePanelsIntegrationTest.focus(f.frames[0],"hudDomainSkill1");
            f.waitFor(()->edt(()->f.skills().targetHint().contains("Fora de alcance")));
            assertTrue(edt(()->f.skills().buttons()[0].isEnabled()),"Aviso estimado não é rejeição autoritativa");f.capture("alvo-fora-de-alcance-previa");
        }
        try(var f=new Fixture("bow_long",522,260,358,260)){
            int enemy=f.caster.snapshot().actors().stream().filter(NetworkSkillPreview::mob).findFirst().orElseThrow().id();
            assertTrue(f.caster.selectTarget(enemy));
            f.waitFor(()->edt(()->f.skills().buttons()[0].getToolTipText().contains("Possível obstáculo")));
            GamePanelsIntegrationTest.focus(f.frames[0],"hudDomainSkill1");
            f.waitFor(()->edt(()->f.skills().targetHint().contains("Possível obstáculo")));f.capture("alvo-sem-linha-de-visao-previa");
        }
    }
    @Test void lowLevelEnergySupportAndDisconnectHaveDistinctFeedbackAndNoPreparedCastLeaks()throws Exception{
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var f=new Fixture("light")){
            assertTrue(f.caster.selectTarget(f.observer.id()));f.waitFor(()->edt(()->f.skills().buttons()[0].isEnabled()));
            f.capture("aliado-suporte-hp");assertFalse(f.caster.selectTarget(2_000_001));
            var sheet=f.caster.playerSheet();var progress=sheet.progress();
            SwingUtilities.invokeAndWait(()->{
                var low=new GameplayProtocol.Sheet(sheet.inventory(),sheet.equipment(),new GameplayProtocol.Progress(progress.xp(),14,progress.nextLevelXp(),progress.attack(),progress.defense(),progress.domain()),sheet.abilities(),sheet.notice());
                f.skills().refresh(low,1000,true);assertFalse(f.skills().buttons()[0].isEnabled());assertTrue(f.skills().buttons()[0].getToolTipText().contains("nível 15"));
                f.skills().activate(1);assertTrue(f.skills().feedbackText().contains("nível 15"));
                f.captureStateOnEdt("fixture-ui-nivel-14");
                f.skills().refresh(sheet,0,true);assertFalse(f.skills().buttons()[0].isEnabled());assertTrue(f.skills().buttons()[0].getToolTipText().contains("Energia insuficiente"));
                f.skills().activate(1);assertTrue(f.skills().feedbackText().contains("Energia insuficiente"));
                f.captureStateOnEdt("fixture-ui-sem-energia");
                f.skills().refresh(sheet,1000,true);
            });
            assertEquals(0,f.caster.skills().cooldowns().get(0),"Prévia de estados não envia conjurações");
            f.waitFor(()->self(f.caster).hp()<self(f.caster).maxHp());
            assertTrue(f.caster.selectTarget(0));assertTrue(f.caster.castSkill(2,0,0,0,0));
            f.waitFor(()->f.caster.skills().events().stream().anyMatch(e->e.skill().equals("light_2")));
            assertTrue(f.caster.selectTarget(f.observer.id()));f.focusWorld(1);SwingKeys.press(KeyEvent.VK_UP);
            f.waitFor(()->self(f.observer).y()<360);SwingKeys.release(KeyEvent.VK_UP);
            assertTrue(f.observer.request(Protocol.WorldAction.TRAVEL,StudioExport.key("portal_","forest/to_village")));
            f.waitFor(()->f.observer.region().equals("village")&&f.caster.selectedTarget()==0);
            assertFalse(f.caster.selectTarget(f.observer.id()),"Outra região não oferece o ator como alvo");f.capture("alvo-em-outra-regiao");
            // A área selecionável será preparada numa segunda sessão real; Luz usa área direta.
            f.caster.close();f.waitFor(()->edt(()->!f.skills().buttons()[0].isEnabled()&&f.skills().feedbackText().contains("Desconectado")));
            assertEquals(0,f.caster.selectedTarget());f.capture("desconectado");assertNull(f.server.failure());
        }
        try(var f=new Fixture("bow_long")){
            f.focusWorld();SwingKeys.tap(KeyEvent.VK_F7);SwingKeys.press(KeyEvent.VK_W);assertTrue(edt(()->f.skills().aiming()));
            f.caster.close();f.waitFor(()->edt(()->!f.skills().aiming()));SwingKeys.release(KeyEvent.VK_W);
            assertFalse(edt(()->f.skills().aiming()));f.capture("desconexao-durante-preparacao");
            assertTrue(f.observer.skills().events().stream().noneMatch(e->e.skill().equals("aetheri_3")));
            f.waitFor(()->f.observer.snapshot().actors().stream().noneMatch(a->a.id()==f.caster.id()));
            try(var next=f.caster.reconnect()){
                SwingUtilities.invokeAndWait(()->{f.frames[0].dispose();f.frames[0]=RpgNetworkClientLauncher.open(next,f.sprites,"Reconexão 224",null,CosmeticCatalog.Selection.none(),frame->{},f.visuals);});
                Task201CharacterSessionTest.waitFor(()->f.ready(next),next,f.observer);
                assertNotEquals(f.caster.id(),next.id());assertEquals(0,next.selectedTarget());
                assertTrue(next.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"bow_long"));
                Task201CharacterSessionTest.waitFor(()->next.playerSheet().progress().domain().equals("bow_long"),next,f.observer);
                var start=self(next);long tick=next.snapshot().tick();
                Task201CharacterSessionTest.waitFor(()->next.snapshot().tick()>tick+6,next,f.observer);
                assertEquals(start.x(),self(next).x(),.01);assertEquals(start.y(),self(next).y(),.01);
                assertFalse(edt(()->f.skills().aiming()));f.capture("reconexao-sem-mira-ou-tecla-presa");
                assertTrue(next.selectTarget(f.observer.id()));f.observer.close();
                Task201CharacterSessionTest.waitFor(()->next.selectedTarget()==0,next);
                f.capture("alvo-desconectado");
            }
        }
    }

    private static <T>T edt(java.util.concurrent.Callable<T> action)throws Exception{return GamePanelsIntegrationTest.onEdt(action);}
    private static Protocol.Actor self(LocalClient c){return GamePanelsIntegrationTest.self(c);}
    /** Recursos temporários e servidor real; fechar não afeta saves pessoais. */
    private final class Fixture implements AutoCloseable {
        final LocalSimulationServer server;final LocalClient caster,observer;final JFrame[] frames=new JFrame[2];
        final RpgPlayerSprites sprites;final RpgNetworkClientLauncher.Presentation visuals;
        final Path evidence=Files.createDirectories(Path.of("../docs/skills-preview-task224/runtime"));
        final String previous=System.getProperty("spiritgate.rpg.dataDir");
        Fixture(String domain)throws Exception{this(domain,220,300,140,300);}
        Fixture(String domain,int enemyX,int enemyY,int playerX,int playerY)throws Exception{
            SwingKeys.drain();System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
            String race=domain.equals("light")?"human":"aetheri";var root=Path.of("..").toAbsolutePath().normalize();
            sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),null);
            var base=WorldContent.baseline();var maps=new java.util.TreeMap<>(base.bundle().maps());var forest=maps.get("forest");
            maps.put("forest",forest.withElements(forest.elements().stream().map(e->e.kind()==WorldDocument.Kind.ENEMY?e.at(e.id(),enemyX,enemyY):e.kind()==WorldDocument.Kind.PLAYER?e.at(e.id(),playerX,playerY):e).toList()));
            var world=new RpgSessionWorld(List.of(race,"human"),null,true,20);world.enableRevisions(LiveRevision.prepare(0,"forest",maps,Task201CharacterSessionTest.EMPTY));
            server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world);server.start();caster=connect("Conjuração 224",race);observer=connect("Aliado 224","human");
            SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(caster,sprites,"TASK-224",null,CosmeticCatalog.Selection.none(),f->{},visuals);frames[1]=RpgNetworkClientLauncher.open(observer,sprites,"Observador 224",null,CosmeticCatalog.Selection.none(),f->{},visuals);});
            waitFor(()->ready(caster)&&ready(observer));assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,domain));
            waitFor(()->caster.playerSheet().progress().domain().equals(domain));waitFor(()->edt(()->skills().buttons()[1].isEnabled()));
        }
        LocalClient connect(String name,String race)throws Exception{return LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);}
        boolean ready(LocalClient c){return c.playerSheet()!=null&&c.skills()!=null&&c.snapshot()!=null&&!c.revisions().blocked();}
        NetworkSkillControls skills(){return (NetworkSkillControls)frames[0].getRootPane().getClientProperty("networkSkillControls");}
        void waitFor(Task201CharacterSessionTest.Check check)throws Exception{Task201CharacterSessionTest.waitFor(check,caster,observer);}
        void focusWorld()throws Exception{focusWorld(0);}
        void focusWorld(int index)throws Exception{SwingUtilities.invokeAndWait(()->{frames[index].toFront();var target=(JComponent)frames[index].getRootPane().getClientProperty("networkPanel");GuiInputTrace.focusRequest(target,target.requestFocusInWindow());});waitFor(()->edt(()->KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()==frames[index].getRootPane().getClientProperty("networkPanel")));}
        void pointer(float x,float y)throws Exception{SwingUtilities.invokeAndWait(()->{var panel=(DebugPanel)frames[0].getRootPane().getClientProperty("networkPanel");var base=panel.aimAt(300,300);var unit=panel.aimAt(301,301);int sx=(int)Math.round(300+(x-base.x)/(unit.x-base.x)),sy=(int)Math.round(300+(y-base.y)/(unit.y-base.y));panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,sx,sy,0,false));});}
        void capture(String name)throws Exception{GamePanelsIntegrationTest.capture(frames[0],evidence.resolve(name+".png"));}
        void captureObserver(String name)throws Exception{GamePanelsIntegrationTest.capture(frames[1],evidence.resolve(name+".png"));}
        /** Captura os estados de UI fornecidos pelo fixture sem modificar a ficha autoritativa real. */
        void captureStateOnEdt(String name){
            try{var hud=(NetworkGameHud)frames[0].getRootPane().getClientProperty("networkGameHud");hud.skillFeedback(skills().feedbackText(),skills().inputHint());GamePanelsIntegrationTest.layout(frames[0]);var image=new java.awt.image.BufferedImage(frames[0].getWidth(),frames[0].getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frames[0].printAll(g);}finally{g.dispose();}javax.imageio.ImageIO.write(image,"PNG",evidence.resolve(name+".png").toFile());}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        }
        public void close()throws Exception{SwingKeys.clear();SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});caster.close();observer.close();server.close();if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);SwingKeys.drain();}
    }
}
