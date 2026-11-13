package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** A ferramenta ocupa uma paleta; o viewport, o cliente e o servidor continuam sendo os da partida. */
class CompactWorldEditorTest {
    @TempDir Path temporary;
    @Test void paintsOriginalGameAndKeepsHudPlayerAndViewport()throws Exception{
        assumeFalse(GraphicsEnvironment.isHeadless());var project=Path.of("..").toAbsolutePath();
        var assets=AssetCatalog.scan(project,null);var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var sprites=RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5);
        var world=new RpgSessionWorld(java.util.List.of("reaper","human"),catalog,true);world.enableRevisions(CorujaoSevenWorld.initial(assets),assets);
        var frameRef=new AtomicReference<JFrame>();var uiRef=new AtomicReference<WorldStudio>();
        var heartbeat=Executors.newSingleThreadScheduledExecutor(r->{var thread=new Thread(r,"editor-test-heartbeat");thread.setDaemon(true);return thread;});
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Editor","reaper","none","none"),false,false,null,assets);
                    var b=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Observador",false,false,null,assets)){
                b.revisions().renderer(p->CompletableFuture.completedFuture(null));
                heartbeat.scheduleAtFixedRate(()->{a.measureLatency();b.measureLatency();},0,200,TimeUnit.MILLISECONDS);
                SwingUtilities.invokeAndWait(()->frameRef.set(RpgNetworkClientLauncher.open(a,sprites,"Teste do editor",catalog,CosmeticCatalog.Selection.none(),f->{})));
                LiveMapEditsTest.await(()->!a.revisions().blocked()&&!b.revisions().blocked());
                var mainFrame=frameRef.get();var content=mainFrame.getContentPane();var original=a.id();String before=world.revisions().activeHash();
                SwingUtilities.invokeAndWait(()->{
                    var ui=new WorldStudio(project);uiRef.set(ui);ui.enterLiveGame(mainFrame,world.revisions(),new LivePublishing(temporary.resolve("versions")));
                    assertSame(content,mainFrame.getContentPane());assertSame(a,mainFrame.getRootPane().getClientProperty("networkClient"));assertNull(ui.liveTools.preview);assertNull(ui.liveTools.game);assertTrue(ui.inGameEditor.palette.getWidth()<=340);assertFalse(ui.isVisible());
                });
                var ui=uiRef.get();var ready=new boolean[1];long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
                do{SwingUtilities.invokeAndWait(()->ready[0]=ui.refresh.isEnabled());if(!ready[0])Thread.sleep(30);}while(!ready[0]&&System.nanoTime()<until);assertTrue(ready[0]);
                SwingUtilities.invokeAndWait(()->{
                    var panel=(DebugPanel)mainFrame.getRootPane().getClientProperty("networkPanel");ui.inGameEditor.setEditing(true);ui.toolSelector.setSelectedItem("Colisao");
                    var point=panel.aimAt(850,450);assertTrue(Double.isFinite(point.x));
                    panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,1,0,850,450,1,false,MouseEvent.BUTTON1));panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,2,0,850,450,1,false,MouseEvent.BUTTON1));
                    assertTrue(ui.document.hitboxes().stream().anyMatch(h->h.id().equals(CollisionTools.cellId(new Point((int)point.x/ui.document.grid()*ui.document.grid(),(int)point.y/ui.document.grid()*ui.document.grid())))),"tool="+ui.toolSelector.getSelectedItem()+" point="+point+" region="+a.region()+" map="+ui.document.region()+" status="+ui.status.getText());
                });
                try{LiveMapEditsTest.await(()->world.revisions().status().phase()==RevisionCoordinator.Phase.COMMITTED&&!world.revisions().activeHash().equals(before)&&world.revisions().activeHash().equals(a.revisions().hash())&&world.revisions().activeHash().equals(b.revisions().hash())&&!a.revisions().blocked()&&!b.revisions().blocked());}
                catch(AssertionError failure){SwingUtilities.invokeAndWait(()->System.err.println("EDITOR="+ui.inGameEditor.status.getText()+" SERVER="+world.revisions().status()+" A="+a.revisions().hash()+" B="+b.revisions().hash()));throw failure;}
                assertEquals(world.revisions().activeHash(),b.revisions().hash());assertEquals(original,a.id());assertEquals(2,server.players());assertFalse(Files.exists(temporary.resolve("versions")));
                SwingUtilities.invokeAndWait(()->{
                    assertSame(content,mainFrame.getContentPane());assertNull(ui.liveTools.preview);
                    var image=new BufferedImage(mainFrame.getWidth(),mainFrame.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();mainFrame.getRootPane().printAll(g);g.dispose();
                    try{Path output=Path.of("target/world-editor-evidence/compact-editor.png");Files.createDirectories(output.getParent());ImageIO.write(image,"png",output.toFile());}catch(Exception error){throw new AssertionError(error);}
                    ui.inGameEditor.setEditing(false);assertNull(mainFrame.getRootPane().getClientProperty("worldEditorActive"));ui.dispose();assertSame(content,mainFrame.getContentPane());assertNull(mainFrame.getRootPane().getClientProperty("inGameMapEditor"));
                });assertTrue(a.connected());assertTrue(b.connected());
            }
        }finally{heartbeat.shutdownNow();SwingUtilities.invokeAndWait(()->{if(uiRef.get()!=null)uiRef.get().dispose();if(frameRef.get()!=null)frameRef.get().dispose();});}
    }
    @Test void arrowIconsHaveDistinctDirectionsAndStates()throws Exception{
        var colors=new java.util.HashSet<Integer>();Path output=Path.of("target/world-editor-evidence/arrows.png");Files.createDirectories(output.getParent());var image=new BufferedImage(240,112,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setColor(new Color(8,27,44));g.fillRect(0,0,240,112);
        for(int row=0;row<2;row++)for(int side=0;side<4;side++){CollisionArrow.draw(g,30+side*60,28+row*56,40,CollisionTools.BITS[side],row==1);colors.add(image.getRGB(30+side*60,28+row*56));}
        g.dispose();ImageIO.write(image,"png",output.toFile());assertTrue(colors.size()>1);assertNotEquals(image.getRGB(30,28),image.getRGB(30,84));
    }
}
