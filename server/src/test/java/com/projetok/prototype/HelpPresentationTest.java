package com.projetok.prototype;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpPresentationTest {
    @Test void helpBindingPublishesExistingValuesAndDoesNotInterfereWithWorld() throws Exception {
        try(var actual=new DebugRuntime(()->DebugWorld.demo(new Random(4)));
            var reference=new DebugRuntime(()->DebugWorld.demo(new Random(4)))) {
            var panel=new DebugPanel(actual::snapshot);
            SwingUtilities.invokeAndWait(()->{
                DebugLauncher.configureKeys(panel,actual,new HashSet<>());
                panel.getActionMap().get("help").actionPerformed(new ActionEvent(panel,0,"help"));
            });
            assertTrue(panel.isHelpVisible());
            var lines=DebugPanel.helpLines(actual.snapshot());
            assertTrue(lines.stream().anyMatch(s->s.contains("HP "+actual.snapshot().playerHp())));
            assertTrue(lines.stream().anyMatch(s->s.contains(actual.snapshot().cryostasisHud().stateText())));
            for(int[] size:new int[][]{{630,375},{1260,750},{2600,1600}}) {
                panel.setSize(size[0],size[1]);var image=new BufferedImage(size[0],size[1],2);var g=image.createGraphics();
                panel.render(g,actual.presentation());g.dispose();
            }
            actual.advanceOneTick();reference.advanceOneTick();assertEquals(reference.presentation(),actual.presentation());
            panel.toggleHelp();assertFalse(panel.isHelpVisible());
        }
    }
}
