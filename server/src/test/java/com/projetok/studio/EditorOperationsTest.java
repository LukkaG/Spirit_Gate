package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EditorOperationsTest {
    static WorldDocument.Element brush(){return new WorldDocument.Element("brush",WorldDocument.Kind.OBJECT,"","",new WorldDocument.Crop(0,0,32,32),0,0,32,32,"ground",true,false,"","FRONT",Map.of());}
    @Test void rectangleClipboardAndUndoRedoAreAtomic(){var d=WorldDocument.empty("mapa",128,128);var h=new EditorHistory(d);var painted=EditorOperations.rectangle(d,brush(),new Point(0,0),new Point(63,63));assertEquals(4,painted.elements().size());h.commit(painted);assertEquals(d,h.undo());assertEquals(painted,h.redo());var copy=EditorOperations.copy(painted,Set.of(painted.elements().get(0).id()));var pasted=EditorOperations.paste(painted,copy,96,96);assertEquals(5,pasted.elements().size());assertEquals(5,pasted.elements().stream().map(WorldDocument.Element::id).distinct().count());assertThrows(IllegalArgumentException.class,()->EditorOperations.paste(d,copy,128,128));}
    @Test void floodIsConnectedAndRefusesOversizeWithoutMutation(){var d=WorldDocument.empty("mapa",128,128);assertEquals(16,EditorOperations.flood(d,brush(),0,0).elements().size());var huge=WorldDocument.empty("mapa",8192,8192);assertThrows(IllegalArgumentException.class,()->EditorOperations.flood(huge,brush(),0,0));assertTrue(huge.elements().isEmpty());}
    @Test void historyIsBoundedAndLockedLayersRefusePaint(){var d=WorldDocument.empty("mapa",128,128);var h=new EditorHistory(d);for(int i=0;i<100;i++){d=EditorOperations.pencil(d,brush(),0,0,true);h.commit(d);}assertEquals(64,h.size());var locked=d.withLayers(d.layers().stream().map(l->new WorldDocument.Layer(l.id(),l.name(),l.role(),l.order(),l.visible(),true)).toList());assertThrows(IllegalArgumentException.class,()->EditorOperations.erase(locked,"ground",0,0));}
}
