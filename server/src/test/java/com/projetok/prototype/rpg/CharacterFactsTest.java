package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CharacterFactsTest {
    @Test void extremeIdentityAndMissingSelectionRemainReadOnly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var identity=new CharacterIdentity(UUID.randomUUID(),"Guardião extremo","guardian","azure","village",1000,100);
            var world=new RpgWorld(identity);var before=world.save();
            var ui=new CharacterSelection();ui.updateSummary(new CharacterSaves.Slot(Path.of("synthetic.properties"),before,"",false));
            assertTrue(ui.resources.getText().contains("1000"));assertTrue(ui.style.getText().contains("Ataque 100"));
            assertEquals(before,world.save());
            ui.updateSummary(null);assertTrue(ui.resources.getText().isBlank());assertTrue(ui.style.getText().isBlank());
            ui.updateSummary(new CharacterSaves.Slot(Path.of("invalid.properties"),null,"Inválido",false));
            assertTrue(ui.resources.getText().isBlank());
        });
    }
}
