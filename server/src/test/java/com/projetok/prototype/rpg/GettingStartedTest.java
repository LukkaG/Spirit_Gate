package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GettingStartedTest {
    @Test void helpReflectsCurrentBindingsWithoutChangingWorld() {
        var keys=new java.util.HashMap<>(RpgControls.DEFAULTS);keys.put("attack","Z");
        var world=new RpgWorld();var before=world.save();var text=GettingStarted.text(new RpgControls(keys));
        assertTrue(text.contains("Z — Golpe"));assertFalse(text.contains("SPACE — Golpe"));assertTrue(text.contains("W A S D"));assertEquals(before,world.save());
    }
}
