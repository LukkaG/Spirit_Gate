package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/** Browser-only entry for a one-click anonymous sprite test. Desktop entry is unchanged. */
public final class BrowserLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            var identity=CharacterIdentity.create("Visitante","guardian","azure");
            RpgLauncher.play(new RpgWorld(identity),new LocalSaveStore(
                    Path.of(System.getProperty("spiritgate.rpg.dataDir","/files/spirit-gate-web"))
                            .resolve("saves/characters/"+identity.id()+".properties")));
        });
    }
}
