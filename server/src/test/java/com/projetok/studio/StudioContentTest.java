package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioContentTest {
    @Test void entitiesComeFromRealCatalogsAndCannotUseUnknownOrWrongRegion(){assertTrue(StudioContent.references(WorldDocument.Kind.NPC).contains("elder"));assertTrue(StudioContent.references(WorldDocument.Kind.CHEST).contains("starter_chest"));var e=new WorldDocument.Element("npc",WorldDocument.Kind.NPC,"","",new WorldDocument.Crop(0,0,32,32),140,320,32,32,"interaction",true,false,"elder","FRONT",Map.of());assertDoesNotThrow(()->StudioContent.validate(e,"village"));assertThrows(IllegalArgumentException.class,()->StudioContent.validate(e,"forest"));assertThrows(IllegalArgumentException.class,()->StudioContent.validate(new WorldDocument.Element("npc",WorldDocument.Kind.NPC,"","",e.crop(),140,320,32,32,"interaction",true,false,"inventado","FRONT",Map.of()),"village"));}
}
