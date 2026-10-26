package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RadminLauncherUiTest {
    @Test void sessionScreensStayResponsiveAndCloseAfterInspection()throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Path output=Path.of("../.task-checkpoints/TASK-176/launcher-radmin");Files.createDirectories(output);
        for(boolean hosting:new boolean[]{true,false}) {
            SwingUtilities.invokeAndWait(()->RadminSessionPanel.open(hosting));
            Thread.sleep(500);
            SwingUtilities.invokeAndWait(()->{
                JFrame frame=null;
                for(Frame candidate:Frame.getFrames())if(candidate instanceof JFrame f&&f.isShowing()&&f.getTitle().equals("Spirit Gate | "+(hosting?"Hospedar partida":"Entrar no mundo")))frame=f;
                assertNotNull(frame);
                var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();
                try{javax.imageio.ImageIO.write(image,"png",output.resolve(hosting?"host.png":"join.png").toFile());}catch(Exception e){throw new RuntimeException(e);}
                frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING));assertFalse(frame.isDisplayable());
            });
        }
    }
}
