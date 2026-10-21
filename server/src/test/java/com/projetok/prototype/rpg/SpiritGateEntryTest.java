package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
class SpiritGateEntryTest {
    @Test void bundledArtAndEntryControlsWork() throws Exception {
        var art=SpiritGateEntry.loadArt();assertNotNull(art);assertTrue(art.getWidth()>1000);assertTrue(art.getWidth()>art.getHeight());
        SwingUtilities.invokeAndWait(()->{var entered=new AtomicBoolean();var panel=new SpiritGateEntry(art,()->entered.set(true));panel.username.setText("Viajante");panel.remember.setSelected(true);panel.enter.doClick();assertTrue(entered.get());assertEquals("Viajante",panel.username.getText());});
    }
    @Test void urbanPortalFormFitsStandardAndCompactWindows() throws Exception {
        var art=SpiritGateEntry.loadArt();
        SwingUtilities.invokeAndWait(()->{
            var panel=new SpiritGateEntry(art,()->{});
            for(var size:new java.awt.Dimension[]{new java.awt.Dimension(1280,720),new java.awt.Dimension(640,480),new java.awt.Dimension(480,540)}){
                panel.setSize(size);SpiritGateEntry.layout(panel);
                var image=new java.awt.image.BufferedImage(size.width,size.height,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();panel.printAll(g);g.dispose();
                for(var control:new JComponent[]{panel.username,panel.remember,panel.enter,panel.status}){
                    var bounds=SwingUtilities.convertRectangle(control.getParent(),control.getBounds(),panel);
                    assertTrue(bounds.width>0&&bounds.height>0);
                    assertTrue(new java.awt.Rectangle(0,0,size.width,size.height).contains(bounds),bounds.toString());
                }
                assertTrue(panel.username.getWidth()>=280);
            }
        });
    }
}
