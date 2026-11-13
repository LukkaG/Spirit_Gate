package com.projetok.domain.mob;

import com.projetok.rpg.RpgSceneSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Protege o contrato do catálogo para fontes locais e futuros adaptadores. */
class MobCatalogTest {
    @Test void loadsExistingSpeciesWithoutApplyingEncounterHpFloor() {
        var catalog = MobCatalog.fromProperties(RpgSceneSet.entries("enemies"));
        assertEquals(40, catalog.get("slime").hp());
        assertEquals(0, catalog.get("warden").respawn());
        assertEquals(100, catalog.get("mob_guardiao").hp());
        assertThrows(UnsupportedOperationException.class, () -> catalog.all().clear());
    }

    @Test void rejectsInvalidDataAndUnbalancedLevels() {
        for (String data : List.of("0|6|65|230|360|240", "40|6|65|230|200|240",
                "40|6|65|230|360|240|2", "40|6|65|230|360|240|"))
            assertThrows(IllegalArgumentException.class,
                    () -> MobCatalog.fromProperties(Map.of("slime", data)));
        var definition = new MobDefinition("slime", 40, 6, 65, 230, 360, 240, 1);
        assertThrows(IllegalArgumentException.class, () -> new MobCatalog(List.of(definition, definition)));
        assertThrows(IllegalArgumentException.class, () -> new MobCatalog(List.of(definition)).get("missing"));
    }
}
