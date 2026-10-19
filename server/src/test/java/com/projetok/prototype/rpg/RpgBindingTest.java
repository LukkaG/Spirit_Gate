package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import javax.swing.JPanel;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class RpgBindingTest {
    @Test void heldKeyDoesNotRepeatAndFocusResetReleases(){var panel=new JPanel();var count=new AtomicInteger();RpgLauncher.bind(panel,"Q",count::incrementAndGet);var press=panel.getActionMap().get("Qtrue");press.actionPerformed(null);press.actionPerformed(null);assertEquals(1,count.get());panel.getActionMap().get("Qfalse").actionPerformed(null);press.actionPerformed(null);assertEquals(2,count.get());RpgLauncher.resetHeld(panel);press.actionPerformed(null);assertEquals(3,count.get());}
}
