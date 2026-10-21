package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GettingStartedTest {
    @Test void helpReflectsCurrentBindingsWithoutChangingWorld() {
        var keys=new java.util.HashMap<>(RpgControls.DEFAULTS);keys.put("attack","Z");
        var world=new RpgWorld();var before=world.save();var text=GettingStarted.text(new RpgControls(keys));
        assertTrue(text.contains("Z — Ataque basico"));assertFalse(text.contains("SPACE — Ataque basico"));assertTrue(text.contains("W A S D"));
        assertTrue(text.contains("nivel 15"));assertTrue(text.contains("nivel 20"));assertEquals(before,world.save());
    }
}
