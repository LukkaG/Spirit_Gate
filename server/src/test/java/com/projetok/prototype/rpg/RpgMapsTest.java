package com.projetok.prototype.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RpgMapsTest {
    @Test void threeRegionsUseExistingMapContract(){for(String id:new String[]{"village","forest","ruins"}){var map=RpgMaps.region(id);assertEquals(960,map.width());assertEquals(2,map.physical().size());assertFalse(map.visuals().isEmpty());}assertNotEquals(RpgMaps.region("village").visuals(),RpgMaps.region("forest").visuals());}
}
