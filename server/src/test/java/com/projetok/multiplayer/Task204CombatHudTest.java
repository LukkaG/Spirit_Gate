package com.projetok.multiplayer;

import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task204CombatHudTest {
    @Test void barsUseServerResourcesAndShowDeathWithoutInventedValues() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var hud=new NetworkCombatHud();var effects=new NetworkCombatPresentation();
            hud.update(null,1,effects);assertTrue(hud.healthText().contains("aguardando"));
            var state=new Protocol.State(1,List.of(new Protocol.Actor(1,"Jogador",140,300,75,130,750,0,false,1,0,1)));
            hud.update(state,1,effects);assertEquals("HP 75 / 130",hud.healthText());
            assertEquals("Energia 75 / 100",hud.energyText());assertTrue(hud.stateText().contains("recuperação"));
            hud.update(new Protocol.State(2,List.of(new Protocol.Actor(1,"Jogador",140,300,0,130,750,0,false))),1,effects);
            assertTrue(hud.stateText().contains("Derrotado"));
            hud.setSize(1100,32);hud.doLayout();var image=new java.awt.image.BufferedImage(1100,32,2);var g=image.createGraphics();
            try{hud.printAll(g);}finally{g.dispose();}
        });
    }
}
