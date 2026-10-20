package com.projetok.prototype.rpg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RpgDataHomeTest {
    @TempDir Path directory;
    @Test void updaterDataDirectoryIsIndependentOfApplicationAndAssetLocation() {
        String previous=System.getProperty("spiritgate.rpg.dataDir");
        try {
            System.clearProperty("spiritgate.rpg.dataDir");assertEquals(RpgMenu.home(),RpgMenu.dataHome());
            System.setProperty("spiritgate.rpg.dataDir",directory.toString());
            assertEquals(directory.toAbsolutePath(),RpgMenu.dataHome());
            assertEquals(RpgMenu.home().resolve("assets/dev/player-base"),RpgPlayerSprites.defaultFolder());
        } finally {
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }
}
