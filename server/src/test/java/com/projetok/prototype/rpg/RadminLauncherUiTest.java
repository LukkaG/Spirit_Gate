package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RadminLauncherUiTest {
    @Test void sessionScreensStayResponsiveAndCloseAfterInspection() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path output=Path.of("../.task-checkpoints/TASK-201/corujao-vii/modal-validation");Files.createDirectories(output);
        String previous=System.getProperty("jpackage.app-path");
        System.setProperty("jpackage.app-path",Path.of("../SpiritGate.exe").toAbsolutePath().normalize().toString());
        try {
            for(boolean hosting:new boolean[]{true,false}) {
                JFrame[] owner={null};JDialog[] modal={null};
                try {
                    SwingUtilities.invokeAndWait(()->{owner[0]=new JFrame("Entrada de teste");owner[0].setSize(640,480);owner[0].setVisible(true);RadminSessionPanel.open(owner[0],hosting,"Viajante");});
                    long deadline=System.nanoTime()+java.time.Duration.ofSeconds(12).toNanos();
                    boolean[] ready={false};
                    while(!ready[0]&&System.nanoTime()<deadline) {
                        Thread.sleep(40);
                        SwingUtilities.invokeAndWait(()->{
                            for(Window candidate:owner[0].getOwnedWindows())if(candidate instanceof JDialog d&&d.isShowing()) {
                                modal[0]=d;ready[0]=containsSelection(d);
                            }
                        });
                    }
                    assertTrue(ready[0],"Modal deve mostrar o formulario carregado, nao uma tela vazia");
                    SwingUtilities.invokeAndWait(()->{
                        JDialog frame=modal[0];assertNotNull(frame);assertTrue(frame.isModal());assertSame(owner[0],frame.getOwner());
                        assertTrue(java.util.Arrays.stream(Frame.getFrames()).noneMatch(f->f.isShowing()&&f.getTitle().equals(frame.getTitle())));
                        var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();
                        try{javax.imageio.ImageIO.write(image,"png",output.resolve(hosting?"host.png":"join.png").toFile());}catch(Exception e){throw new RuntimeException(e);}
                        frame.getRootPane().getActionMap().get("cancelSession").actionPerformed(new java.awt.event.ActionEvent(frame,0,"cancel"));
                        assertFalse(frame.isDisplayable());assertTrue(owner[0].isDisplayable());
                    });
                } finally {SwingUtilities.invokeAndWait(()->{if(modal[0]!=null)modal[0].dispose();if(owner[0]!=null)owner[0].dispose();});}
            }
        } finally {if(previous==null)System.clearProperty("jpackage.app-path");else System.setProperty("jpackage.app-path",previous);}
    }
    private static boolean containsSelection(Container parent) {
        if(parent instanceof SessionCharacterCreation)return parent.getWidth()>0&&parent.getHeight()>0;
        for(Component child:parent.getComponents())if(child instanceof Container nested&&containsSelection(nested))return true;
        return false;
    }
}
