package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class RpgContentTest {
    private RpgContent load(String text) throws IOException { return RpgContent.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))); }
    @Test void loadsTypedReferences() { assertEquals("Bosque dos Ecos",RpgContent.builtin().get("region","forest").name()); }
    @Test void rejectsMalformedDuplicatesNonFiniteAndExcess() {
        for (String text : new String[]{"schema=2", "schema=1\nregion.a=A|a|NaN", "schema=1\nregion.a=A|a|1\nregion.a=A|a|2", "schema=1\nregion.a=A|missing|0", "x".repeat(65537)})
            assertThrows(IllegalArgumentException.class,()->load(text));
        assertThrows(IllegalArgumentException.class,()->RpgContent.builtin().get("item","missing"));
    }
}
