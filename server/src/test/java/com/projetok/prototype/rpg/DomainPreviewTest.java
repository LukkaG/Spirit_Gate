package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class DomainPreviewTest {
    @Test void compatiblePathsAndPermanentSelectionRespectInitiation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var race : CharacterRaces.all()) {
                var before = new DomainPreview(race.id(), 14, "none", true);
                assertFalse(before.choose.isEnabled());
                var available = new DomainPreview(race.id(), 15, "none", true);
                assertTrue(available.choose.isEnabled());
                assertEquals(Domains.forRace(race.id()).size(), available.domains.getItemCount());
                available.setSize(640, 480); layout(available);
                assertTrue(available.scene.getWidth() > 0);
                assertTrue(available.scene.getHeight() > 80, "A descrição não pode ocultar a prévia inicial");
                var chosen = new DomainPreview(race.id(), 30, Domains.forRace(race.id()).get(0).id(), true);
                assertFalse(chosen.choose.isEnabled());
                var browse = new DomainPreview(race.id(), 15, "none", false);
                assertFalse(browse.choose.isVisible());
                for (int i = 0; i < available.domains.getItemCount(); i++) {
                    available.domains.setSelectedIndex(i);
                    assertEquals(race.id(), Domains.get(available.selectedDomain()).race());
                    assertFalse(available.description.getText().isBlank());
                    available.scene.setSize(600, 260);
                    var image = new BufferedImage(600, 260, BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); available.scene.paint(graphics); graphics.dispose();
                    assertNotEquals(0, image.getRGB(120, 100));
                }
            }
            assertEquals("1,5", DomainPreview.formatXp(3));
            assertEquals("3", DomainPreview.formatXp(6));
        });
    }
    private static void layout(java.awt.Container root) {
        root.doLayout();
        for (var child : root.getComponents()) if (child instanceof java.awt.Container nested) layout(nested);
    }
}
