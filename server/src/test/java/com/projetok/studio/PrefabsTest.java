package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PrefabsTest {
    @Test void relativeCollisionsFreshIdsAndUndoSurvivePersistence()throws Exception{var d=WorldDocument.empty("mapa",256,256);d=EditorOperations.pencil(d,EditorOperationsTest.brush(),32,32,true).withHitboxes(List.of(new WorldDocument.Hitbox("box",32,48,32,16,true,"collision")));var p=Prefabs.capture(d,Set.of(d.elements().get(0).id()),"casa");assertEquals(0,p.elements().get(0).x());assertEquals(16,p.hitboxes().get(0).y());assertEquals(p,DocumentStore.decode(DocumentStore.encode(p)));var h=new EditorHistory(d);var inserted=Prefabs.insert(d,p,128,128);h.commit(inserted);assertEquals(2,inserted.elements().stream().map(WorldDocument.Element::id).distinct().count());assertEquals(2,inserted.hitboxes().size());assertEquals(d,h.undo());assertThrows(IllegalArgumentException.class,()->Prefabs.insert(h.current(),p,250,250));}
}
