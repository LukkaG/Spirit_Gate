package com.projetok.prototype;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusControlsTest {
    private static void action(DebugPanel panel,String name) {
        panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name));
    }
    @Test void focusLossReleasesKeysAndAimAndAllowsNewPress() throws Exception {
        try(var runtime=new DebugRuntime(()->DebugWorld.demo(new Random(3)))) {
            var panel=new DebugPanel(runtime::snapshot);var held=new HashSet<String>();
            Runnable[] release=new Runnable[1];
            SwingUtilities.invokeAndWait(()->{
                release[0]=DebugLauncher.configureKeys(panel,runtime,held);
                action(panel,"press_D");action(panel,"press_E");
            });
            runtime.advanceOneTick();float x=runtime.snapshot().actors().get(0).x();
            SwingUtilities.invokeAndWait(release[0]);runtime.advanceOneTick();
            assertEquals(x,runtime.snapshot().actors().get(0).x());assertTrue(held.isEmpty());
            SwingUtilities.invokeAndWait(()->action(panel,"press_SHIFT"));runtime.advanceOneTick();
            assertFalse(runtime.snapshot().lightningStepActive());
            assertEquals(DebugSnapshot.SkillFeedbackKind.INVALID_TARGET,runtime.snapshot().lightningStepHud().feedback().kind());
            SwingUtilities.invokeAndWait(()->{action(panel,"release_SHIFT");action(panel,"press_D");});
            runtime.advanceOneTick();assertTrue(runtime.snapshot().actors().get(0).x()>x);
        }
    }
    @Test void hudAndLetterboxNeverBecomeTargetsAtAnyZoom() {
        var snapshot=DebugWorld.demo().snapshot();var panel=new DebugPanel(()->snapshot);
        for(int zoom=1;zoom<=4;zoom++) {
            panel.setCameraZoom(zoom);panel.setSize(2600,1600);var view=panel.viewport(snapshot);
            assertTrue(Double.isNaN(panel.aimAt(0,0).x));
            assertTrue(Double.isNaN(panel.aimAt((float)(view.offsetX()+1000*view.scale()),(float)(view.offsetY()+100*view.scale())).x));
            var screen=view.worldToScreen(view.originX()+20,view.originY()+20);
            assertEquals(view.originX()+20,panel.aimAt((float)screen.x,(float)screen.y).x,1e-4);
        }
    }
}
