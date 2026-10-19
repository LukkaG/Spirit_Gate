package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RpgWorldTest {
    @Test void movementUsesExistingWallsAndBounds() {
        RpgWorld world = new RpgWorld();
        for (int i=0;i<200;i++) world.tick(-1,0);
        assertTrue(world.snapshot().x()>=18);
        assertEquals(200,world.snapshot().ticks());
    }
    @Test void invalidMovementDoesNotAdvanceTick() {
        RpgWorld world = new RpgWorld();
        assertThrows(IllegalArgumentException.class,()->world.tick(Float.NaN,0));
        assertEquals(0,world.snapshot().ticks());
    }
}
