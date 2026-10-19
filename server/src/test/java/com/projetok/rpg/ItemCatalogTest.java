package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ItemCatalogTest {
    @Test void categoryRestrictionsAreValidated() {
        var catalog=new ItemCatalog(RpgContent.builtin()); assertEquals(5,catalog.all().size());
        assertEquals(50,catalog.get("potion").healing());
        assertThrows(IllegalArgumentException.class,()->new ItemCatalog.Item("bad","Name","Description",ItemCatalog.Category.MATERIAL,ItemCatalog.Rarity.RARE,1,1,"weapon",0,0,1));
        assertThrows(IllegalArgumentException.class,()->catalog.get("missing"));
    }
}
