package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicReference;

class CharacterCreationTest {
    @Test void validationAndClassPreviewUseDomain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var form = new CharacterCreation(null);
            var accepted = new AtomicReference<CharacterIdentity>();
            form.name.setText("!"); form.submit(accepted::set);
            assertNull(accepted.get()); assertFalse(form.error.getText().isBlank());
            form.mystic.doClick(); form.appearance.setSelectedItem("amber");
            assertEquals("mystic", form.preview.slot.state().identity().archetype());
            assertEquals(Archetypes.get("mystic").hp(), form.preview.slot.state().hp());
            var previous=form.preview.direction;form.preview.orientation().doClick();assertNotEquals(previous,form.preview.direction);
            form.name.setText("Heroi teste"); form.submit(accepted::set);
            assertEquals("mystic", accepted.get().archetype());
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
