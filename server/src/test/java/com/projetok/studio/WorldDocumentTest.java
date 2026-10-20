package com.projetok.studio;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorldDocumentTest {
    @Test void boundedImmutableDocumentSeparatesGridAndSpriteDimensions(){var d=WorldDocument.empty("mapa",960,640);assertEquals(8,d.layers().size());assertThrows(UnsupportedOperationException.class,()->d.layers().clear());assertThrows(IllegalArgumentException.class,()->WorldDocument.empty("../mapa",960,640));assertThrows(IllegalArgumentException.class,()->WorldDocument.empty("mapa",Integer.MAX_VALUE,640));assertEquals(153,new WorldDocument.Crop(0,0,153,153).width());}
}
