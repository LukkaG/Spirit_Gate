package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioLayersTest {
    @Test void orderAndVisibilityAreDeterministicAndUndoable(){var d=WorldDocument.empty("mapa",128,128);var h=new EditorHistory(d);var first=d.layers().get(0);var next=d.withLayers(d.layers().stream().map(l->l.id().equals(first.id())?new WorldDocument.Layer(l.id(),"Novo",l.role(),99,false,l.locked()):l).toList());assertEquals(first.id(),next.orderedLayers().get(7).id());h.commit(next);assertEquals(d,h.undo());assertThrows(IllegalArgumentException.class,()->EditorOperations.pencil(next,EditorOperationsTest.brush(),0,0,true));}
}
