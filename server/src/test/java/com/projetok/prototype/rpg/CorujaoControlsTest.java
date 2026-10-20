package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CorujaoControlsTest {
    private static void key(JPanel panel,String name){panel.getActionMap().get(name).actionPerformed(new ActionEvent(panel,0,name));}
    @Test void rapidPressesHeldKeysAndFocusResetDoNotDuplicateActions() {
        JPanel panel=new JPanel();AtomicInteger count=new AtomicInteger();RpgLauncher.bind(panel,"SPACE",count::incrementAndGet);
        for(int i=0;i<30;i++)key(panel,"SPACEtrue");assertEquals(1,count.get());
        key(panel,"SPACEfalse");key(panel,"SPACEtrue");assertEquals(2,count.get());
        RpgLauncher.resetHeld(panel);key(panel,"SPACEtrue");assertEquals(3,count.get());
        for(int i=0;i<20;i++){key(panel,"SPACEfalse");key(panel,"SPACEtrue");}assertEquals(23,count.get());
    }
    @Test void commandCapacityAndSimulationCooldownStayBounded() {
        AtomicInteger count=new AtomicInteger();try(var runtime=new RpgRuntime()) {
            for(int i=0;i<64;i++)assertTrue(runtime.command(w->count.incrementAndGet()));
            assertFalse(runtime.command(w->count.incrementAndGet()));runtime.advanceOneTick();assertEquals(64,count.get());
        }
        var w=CorujaoVisualStateTest.forest();assertTrue(w.attack());
        for(int i=0;i<200;i++)assertFalse(w.attack());
        assertEquals(1,w.snapshot().events().stream().filter(e->e.kind()==com.projetok.rpg.RpgCombatEvent.Kind.ATTACK).count());
    }
}
