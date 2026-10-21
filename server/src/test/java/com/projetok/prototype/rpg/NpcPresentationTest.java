package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NpcPresentationTest {
    @Test void realNpcDialogueRendersAndClosesThroughRuntime() throws Exception {
        var world=new RpgWorld();assertTrue(world.interact());
        try(var runtime=new RpgRuntime(world)) {
            SwingUtilities.invokeAndWait(()->{
                var hud=new RpgHud(runtime);hud.update(runtime.snapshot());
                assertTrue(has(hud,"Escape: fechar"));
                var close=find(hud);assertNotNull(close);close.doClick();
            });
            runtime.advanceOneTick();assertNull(runtime.snapshot().dialogue());
            SwingUtilities.invokeAndWait(()->{var hud=new RpgHud(runtime);hud.update(runtime.snapshot());assertFalse(has(hud,"Escape: fechar"));});
        }
    }
    private static boolean has(java.awt.Container c,String text){for(var child:c.getComponents()){if(child instanceof JLabel label&&label.getText().contains(text))return true;if(child instanceof java.awt.Container nested&&has(nested,text))return true;}return false;}
    private static JButton find(java.awt.Container c){for(var child:c.getComponents()){if(child instanceof JButton button&&button.getText().equals("Fechar"))return button;if(child instanceof java.awt.Container nested){var button=find(nested);if(button!=null)return button;}}return null;}
}
