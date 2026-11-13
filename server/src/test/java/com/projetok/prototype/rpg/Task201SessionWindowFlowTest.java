package com.projetok.prototype.rpg;

import com.projetok.multiplayer.*;
import com.projetok.studio.AssetCatalog;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Prova grafica com servidor e dois clientes TCP, usando os componentes de producao. */
class Task201SessionWindowFlowTest {
    @TempDir Path temporary;

    @Test void modalHandoffAndReconnectKeepOnlyGamesAndPreserveHostUntilLastWindowCloses() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path project=Path.of("..").toAbsolutePath().normalize();
        Path evidence=Files.createTempDirectory(Files.createDirectories(project.resolve(".task-checkpoints/TASK-201/corujao-vii/ui-flow")),"run-");
        var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var human=RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"),250,.5);
        var visuals=new NetworkPlayerVisuals(project.resolve("assets/dev/reaper"),human);
        var preview=catalog.onLogicalCanvas(64,64,32,60,NetworkPlayerVisuals.LEGACY_SCALE);
        var assets=AssetCatalog.scan(project,null);
        String prior=System.getProperty("spiritgate.rpg.dataDir");
        System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        AtomicReference<LocalClient> a=new AtomicReference<>(),b=new AtomicReference<>();
        AtomicReference<JFrame> gameA=new AtomicReference<>(),gameB=new AtomicReference<>();
        AtomicReference<Throwable> failure=new AtomicReference<>();
        JFrame[] entry=new JFrame[2];JDialog[] modal=new JDialog[2];
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,catalog,project)) {
            for(int index=0;index<2;index++) {
                int slot=index;var destination=index==0?a:b;var game=index==0?gameA:gameB;
                var lifecycle=new SessionGameLifecycle(index==0?server:null,project);
                JButton[] confirm={null};
                SwingUtilities.invokeAndWait(()->{
                    entry[slot]=new JFrame("Entrada "+slot);SessionWindowNavigation.mark(entry[slot]);
                    entry[slot].setSize(640,420);entry[slot].setVisible(true);
                    modal[slot]=new JDialog(entry[slot],"Personagem da sessao",Dialog.ModalityType.DOCUMENT_MODAL);
                    var form=new SessionCharacterCreation(visuals,catalog,preview);form.setPlayerName("Mesmo Nome");
                    form.setSelectedRace(slot==0?"human":"reaper");
                    JPanel content=new JPanel(new BorderLayout());content.add(form);
                    confirm[0]=new JButton("Conectar");content.add(confirm[0],BorderLayout.SOUTH);
                    confirm[0].addActionListener(e->{
                        var choice=form.selection();confirm[0].setEnabled(false);
                        new SwingWorker<LocalClient,Void>() {
                            @Override protected LocalClient doInBackground() throws Exception {
                                return LocalClient.character(InetAddress.getLoopbackAddress(),server.port(),choice,false,false,null,assets);
                            }
                            @Override protected void done() {
                                try {
                                    LocalClient client=get();destination.set(client);
                                    JFrame opened=RpgNetworkClientLauncher.open(client,human,"127.0.0.1:"+server.port(),catalog,choice.cosmetics(),frame->{
                                        lifecycle.adopt(frame);game.set(frame);destination.set((LocalClient)frame.getRootPane().getClientProperty("networkClient"));
                                    },new RpgNetworkClientLauncher.Presentation(visuals,preview));
                                    SessionWindowNavigation.enterGame(opened,modal[slot]);
                                } catch(Throwable error) {failure.set(error);}
                            }
                        }.execute();
                    });
                    modal[slot].setContentPane(content);modal[slot].setSize(900,620);modal[slot].setLocation(20+slot*35,20+slot*35);
                    SwingUtilities.invokeLater(()->modal[slot].setVisible(true));
                });
                waitFor(()->onEdt(()->modal[slot].isShowing()));
                capture(modal[slot],evidence.resolve("selection-"+slot+".png"));
                SwingUtilities.invokeAndWait(()->confirm[0].doClick());
                waitFor(()->failure.get()!=null||game.get()!=null);
                assertNull(failure.get(),()->String.valueOf(failure.get()));
                waitFor(()->onEdt(()->!entry[slot].isDisplayable()&&!modal[slot].isDisplayable()));
                assertTrue(server.running(),"Fechar a entrada nao pode fechar o servidor");
                assertTrue(destination.get().connected());
            }
            a.get().await(state->state.actors().stream().filter(actor->!actor.boss()).count()==2,Duration.ofSeconds(8));
            b.get().await(state->state.actors().stream().filter(actor->!actor.boss()).count()==2,Duration.ofSeconds(8));
            assertNotEquals(a.get().id(),b.get().id());
            assertEquals("human",a.get().appearances().get(a.get().id()).race());
            assertEquals("reaper",a.get().appearances().get(b.get().id()).race());
            assertEquals(a.get().appearances(),b.get().appearances());
            assertTrue(onEdt(()->((NetworkPlayerVisuals)gameB.get().getRootPane().getClientProperty("networkVisuals")).usesReaper("reaper")),"O jogo deve usar o atlas Reaper verificado pela previa");
            assertEquals(2,onEdtInt(()->(int)java.util.Arrays.stream(Frame.getFrames()).filter(f->f.isShowing()&&f.getTitle().startsWith("Spirit Gate | RPG LOCAL")).count()));
            capture(gameA.get(),evidence.resolve("game-human.png"));capture(gameB.get(),evidence.resolve("game-reaper.png"));

            int oldId=a.get().id();JFrame oldWindow=gameA.get();a.get().close();
            b.get().await(state->state.actors().stream().noneMatch(actor->actor.id()==oldId),Duration.ofSeconds(5));
            waitFor(()->onEdt(()->findButton(oldWindow,"Reconectar nova sessão").isEnabled()));
            SwingUtilities.invokeAndWait(()->findButton(oldWindow,"Reconectar nova sessão").doClick());
            waitFor(()->gameA.get()!=oldWindow);
            assertTrue(server.running(),"Descartar janela antiga da reconexao nao encerra host");
            assertFalse(onEdt(oldWindow::isDisplayable));
            assertNotEquals(oldId,a.get().id());assertEquals("human",a.get().characterSelection().race());
            a.get().await(state->state.actors().stream().filter(actor->!actor.boss()).count()==2,Duration.ofSeconds(8));
            capture(gameA.get(),evidence.resolve("reconnected-human.png"));
            SwingUtilities.invokeAndWait(()->gameB.get().dispose());
            assertTrue(server.running());
            SwingUtilities.invokeAndWait(()->gameA.get().dispose());
            waitFor(()->!server.running());
            Files.writeString(evidence.resolve("result.json"),"{\"realTcpClients\":2,\"sameNamesDistinctIds\":true,\"firstRace\":\"human\",\"secondRace\":\"reaper\",\"navigationClosed\":true,\"hostPreservedOnHandoff\":true,\"hostPreservedOnReconnect\":true,\"hostClosedWithFinalWindow\":true,\"physicalTwoComputers\":false}");
        } finally {
            if(a.get()!=null)a.get().close();if(b.get()!=null)b.get().close();
            SwingUtilities.invokeAndWait(()->{for(JDialog w:modal)if(w!=null)w.dispose();for(JFrame w:entry)if(w!=null)w.dispose();if(gameA.get()!=null)gameA.get().dispose();if(gameB.get()!=null)gameB.get().dispose();});
            if(prior==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",prior);
        }
    }

    private static JButton findButton(Container parent,String text) {
        for(Component c:parent.getComponents()) {
            if(c instanceof JButton b&&b.getText().equals(text))return b;
            if(c instanceof Container nested) {JButton b=findButton(nested,text);if(b!=null)return b;}
        }
        return null;
    }
    private static boolean onEdt(BooleanSupplier action) {
        boolean[] value={false};try{SwingUtilities.invokeAndWait(()->value[0]=action.getAsBoolean());}catch(Exception e){throw new RuntimeException(e);}return value[0];
    }
    private static int onEdtInt(java.util.function.IntSupplier action) {
        int[] value={0};try{SwingUtilities.invokeAndWait(()->value[0]=action.getAsInt());}catch(Exception e){throw new RuntimeException(e);}return value[0];
    }
    private static void waitFor(BooleanSupplier condition) throws Exception {
        long end=System.nanoTime()+Duration.ofSeconds(12).toNanos();
        while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(25);
        assertTrue(condition.getAsBoolean(),"Condicao grafica/servidor nao confirmou no prazo");
    }
    private static void capture(Window window,Path file) throws Exception {
        SwingUtilities.invokeAndWait(()->{var image=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();window.printAll(g);g.dispose();try{ImageIO.write(image,"png",file.toFile());}catch(Exception e){throw new RuntimeException(e);}});
    }
}
