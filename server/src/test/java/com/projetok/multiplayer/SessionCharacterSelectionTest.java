package com.projetok.multiplayer;

import com.projetok.prototype.rpg.CosmeticCatalog;
import com.projetok.studio.WorldContent;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contrato de criacao efemera e compatibilidade binaria das aplicacoes anteriores. */
class SessionCharacterSelectionTest {
    static CosmeticCatalog catalog() throws Exception {
        return CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));
    }

    static RpgSessionWorld world() throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog(),true);
        world.enableRevisions(WorldContent.baseline());
        return world;
    }

    static byte[] bytes(Protocol.Message message) throws Exception {
        var out=new ByteArrayOutputStream();Protocol.write(out,message);return out.toByteArray();
    }

    @Test void normalizesNameAndRecognizesFourRacesWithoutPersistentIdentity() {
        for(String race:List.of("reaper","human","aetheri","spirit")) {
            var value=new SessionCharacterSelection(" Joa\u0303o ",race,"none","none");
            assertEquals("João",value.name());assertEquals(race,value.race());
            assertEquals(CosmeticCatalog.Selection.none(),value.cosmetics());
        }
        assertThrows(IllegalArgumentException.class,()->new SessionCharacterSelection("Ab","human","none","none"));
        assertThrows(IllegalArgumentException.class,()->new SessionCharacterSelection("1Invalido","human","none","none"));
        assertThrows(IllegalArgumentException.class,()->new SessionCharacterSelection("Valido","guardian","none","none"));
        assertThrows(IllegalArgumentException.class,()->new SessionCharacterSelection("Valido","human","../saves","none"));
        assertThrows(IllegalArgumentException.class,()->new SessionCharacterSelection("Valido","reaper","hair_short","none"));
    }

    @Test void characterFrameRoundTripsAndHistoricalGoldensAreUnchanged() throws Exception {
        var choice=new Protocol.CharacterCreate("human","none","none");
        assertEquals("0000001201100568756d616e046e6f6e65046e6f6e65",HexFormat.of().formatHex(bytes(choice)));
        assertEquals(choice,Protocol.read(new ByteArrayInputStream(bytes(choice))));
        for(int application=1;application<=4;application++) {
            var hello=new Protocol.Hello(application,"Viajante");
            assertEquals("0000000f01010000000"+application+"085669616a616e7465",HexFormat.of().formatHex(bytes(hello)));
        }
        assertEquals("0000000b0104000000000000000700",HexFormat.of().formatHex(bytes(new Protocol.State(7,List.of()))));
        assertThrows(IllegalArgumentException.class,()->new Protocol.RevisionCommand("a".repeat(64),choice));
    }

    @Test void rejectsTruncatedAndExtraProfileBytes() throws Exception {
        byte[] original=bytes(new Protocol.CharacterCreate("spirit","none","none"));
        for(int size=0;size<original.length;size++) {
            byte[] truncated=Arrays.copyOf(original,size);
            assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(truncated)));
        }
        byte[] extra=Arrays.copyOf(original,original.length+1);
        java.nio.ByteBuffer.wrap(extra).putInt(original.length-3);
        assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(extra)));
    }

    @Test void authorityValidatesCatalogAndCreatesDistinctIdentitiesForSameNameAndRace() throws Exception {
        var world=world();
        var unavailable=new SessionCharacterSelection("Mesmo Nome","human","missing","none");
        assertFalse(world.join(1,unavailable,5));assertTrue(world.snapshot().actors().isEmpty());
        var selected=new SessionCharacterSelection("Mesmo Nome","human","hair_short","jacket");
        assertTrue(world.join(1,selected,5));assertTrue(world.join(2,selected,5));
        assertEquals("human",world.race(1));assertEquals("human",world.race(2));
        assertNotEquals(world.sessionIdentity(1),world.sessionIdentity(2));
        var first=world.sessionIdentity(1);
        assertFalse(world.join(1,new SessionCharacterSelection("Outro Nome","spirit","none","none"),5));
        assertEquals(first,world.sessionIdentity(1));assertEquals("human",world.race(1));
        assertFalse(world.join(3,selected,5));assertEquals(2,world.snapshot().actors().size());
        assertEquals("hair_short",world.appearances().get(0).hair());
        assertFalse(new RpgSessionWorld().join(3,selected,5));
        assertFalse(world().join(1,"Sem Perfil",5));
        world.closed();
    }

    @Test void raceCosmeticRestrictionAppliesToNewCharactersWithoutChangingLegacyContract() throws Exception {
        var selected=world();assertTrue(selected.join(1,new SessionCharacterSelection("Novo Reaper","reaper","none","none"),5));
        selected.freeze(false);
        assertFalse(selected.appearance(1,new Protocol.AppearanceRequest(1,"hair_short","jacket")));
        assertEquals(CosmeticCatalog.Selection.none(),new CosmeticCatalog.Selection(selected.appearances().get(0).hair(),selected.appearances().get(0).clothing()));
        var legacy=new RpgSessionWorld(List.of("reaper","human"),catalog());
        assertTrue(legacy.join(1,"Antigo Reaper",2));
        assertTrue(legacy.appearance(1,new Protocol.AppearanceRequest(1,"hair_short","jacket")));
        assertEquals("reaper",legacy.race(1));selected.closed();
    }
}
