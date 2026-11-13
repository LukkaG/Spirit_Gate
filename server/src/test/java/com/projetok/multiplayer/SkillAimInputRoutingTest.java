package com.projetok.multiplayer;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reset de contexto precisa parar a mira, sem reaproveitar teclas mantidas como caminhada. */
class SkillAimInputRoutingTest {
    @Test void resetStopsReticleBeforeQuarantineAndNewPressRestoresNormalMovement()throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        try(var fixture=new GameInputRoutingTest.Fixture()){
            var aiming=new AtomicBoolean(true);var reticleMoving=new AtomicBoolean();
            SwingUtilities.invokeAndWait(()->fixture.input.aiming(aiming::get,(code,pressed)->reticleMoving.set(pressed),()->aiming.set(false)));
            fixture.focus(fixture.world);SwingKeys.press(KeyEvent.VK_W);
            assertTrue(reticleMoving.get());assertFalse(fixture.moving.get());
            SwingUtilities.invokeAndWait(fixture.input::reset);
            assertFalse(reticleMoving.get(),"Reset precisa zerar a direção da mira antes de bloquear o release");
            aiming.set(false);SwingKeys.press(KeyEvent.VK_W);assertFalse(fixture.moving.get(),"Autorrepetição não deve mover após trocar de contexto");
            SwingKeys.release(KeyEvent.VK_W);SwingKeys.tap(KeyEvent.VK_W);
            assertEquals(1,fixture.moveStarts.get());assertFalse(fixture.moving.get());assertFalse(reticleMoving.get());
        }
    }
}
