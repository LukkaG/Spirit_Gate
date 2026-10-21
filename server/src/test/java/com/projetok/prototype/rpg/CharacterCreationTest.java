package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicReference;

class CharacterCreationTest {
    @Test void validationAndRacePreviewStartWithoutDomain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var form = new CharacterCreation(null);
            var accepted = new AtomicReference<CharacterIdentity>();
            form.name.setText("!"); form.submit(accepted::set);
            assertNull(accepted.get()); assertFalse(form.error.getText().isBlank());
            assertEquals(4, form.races.size());
            for (var race : CharacterRaces.all()) {
                form.races.get(race.id()).doClick();
                assertEquals(race.id(), form.preview.slot.state().identity().race());
                assertEquals(race.hp(), form.preview.slot.state().hp());
                assertFalse(DomainProgression.read(form.preview.slot.state().flags()).chosen());
                assertTrue(form.origin.getText().contains(race.startingAttackName()));
            }
            form.races.get("aetheri").doClick(); form.appearance.setSelectedItem("amber");
            var previous=form.preview.direction;form.preview.orientation().doClick();assertNotEquals(previous,form.preview.direction);
            form.name.setText("Heroi teste"); form.submit(accepted::set);
            assertEquals("aetheri", accepted.get().race());
            assertEquals("amber", accepted.get().appearance());
        });
    }
    @Test void fallbackPaintAndCancelDoNotCreate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var form = new CharacterCreation(null);
            var accepted = new AtomicReference<CharacterIdentity>();
            form.create.addActionListener(e -> form.submit(accepted::set));
            form.back.doClick(); assertNull(accepted.get());
            for (var size : new int[][]{{1000,620},{800,600},{640,480}}) {
                form.setSize(size[0],size[1]); layout(form);
                var image = new java.awt.image.BufferedImage(size[0],size[1],2);
                var graphics = image.createGraphics(); form.printAll(graphics); graphics.dispose();
                assertTrue(form.create.getWidth()>0); assertTrue(form.preview.getHeight()>0);
                assertTrue(form.attributes.getHeight()>=form.attributes.getPreferredSize().height, "Atributos não podem ser cortados");
            }
        });
    }
    private static void layout(java.awt.Container c) {c.doLayout(); for(var child:c.getComponents()) if(child instanceof java.awt.Container nested) layout(nested);}
}
