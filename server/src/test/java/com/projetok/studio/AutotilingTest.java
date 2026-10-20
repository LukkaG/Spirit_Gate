package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AutotilingTest {
    @Test void cornersNeighborsFallbackUndoAndRoundTrip()throws Exception{var base=WorldDocument.empty("mapa",128,128);var fallback=new WorldDocument.Crop(0,0,32,32);var edge=new WorldDocument.Crop(32,0,32,32);base=Autotiling.register(base,new Autotiling.Rule("agua","","",fallback,Map.of(2,edge)));var brush=Autotiling.brush(base,"agua","ground");var a=EditorOperations.pencil(base,brush,0,0,true);var both=Autotiling.refresh(EditorOperations.pencil(a,brush,32,0,true));assertEquals(edge,both.elements().get(0).crop());assertEquals(fallback,both.elements().get(1).crop());assertFalse(Autotiling.missing(both).isEmpty());var h=new EditorHistory(a);h.commit(both);assertEquals(a,h.undo());assertEquals(both,h.redo());assertEquals(both,DocumentStore.decode(DocumentStore.encode(both)));var removed=Autotiling.refresh(EditorOperations.erase(both,"ground",32,0));assertEquals(fallback,removed.elements().get(0).crop());}
}
