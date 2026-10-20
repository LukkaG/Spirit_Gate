package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GroupEditingTest {
    @Test void atomicBoundsLocksDistributionAndHistory(){var d=WorldDocument.empty("mapa",256,256);for(int x:new int[]{0,32,128})d=EditorOperations.pencil(d,EditorOperationsTest.brush(),x,32,true);var ids=new HashSet<>(d.elements().stream().map(WorldDocument.Element::id).toList());var original=d;assertThrows(IllegalArgumentException.class,()->GroupEditing.move(original,ids,250,0));var arranged=GroupEditing.arrange(d,ids,true,true);assertEquals(64,arranged.elements().get(1).x());var locked=GroupEditing.lock(d,ids,true);assertThrows(IllegalArgumentException.class,()->GroupEditing.delete(locked,ids));assertEquals(0,GroupEditing.delete(GroupEditing.lock(locked,ids,false),ids).elements().size());var h=new EditorHistory(d);h.commit(GroupEditing.move(d,ids,0,32));assertEquals(d,h.undo());}
}
