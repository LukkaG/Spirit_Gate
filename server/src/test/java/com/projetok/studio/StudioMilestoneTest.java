package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioMilestoneTest {
    @Test void firstMapIsSerializableAndCompatibleWithRealRpg()throws Exception{var d=StudioExample.document();assertEquals(d,DocumentStore.decode(DocumentStore.encode(d)));assertTrue(StudioValidator.validate(Map.of(d.id(),d),new AssetCatalog.Result(List.of(),List.of(),List.of()),true).isEmpty());assertDoesNotThrow(()->StudioExport.scenes(d.id(),Map.of(d.id(),d)));}
}
