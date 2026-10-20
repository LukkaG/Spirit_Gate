package com.projetok.studio;
import java.awt.Rectangle;
import java.util.*;

/** Indice imutavel de viewport com ordem original e memoria de referencias limitada. */
final class CanvasIndex {
    record Entry(int order,WorldDocument.Element element){}
    final Map<Integer,java.util.List<Entry>> cells=new HashMap<>();final java.util.List<Entry> large=new ArrayList<>();
    CanvasIndex(WorldDocument d){int count=0;for(int i=0;i<d.elements().size();i++){var e=d.elements().get(i);var entry=new Entry(i,e);int x0=e.x()/256,y0=e.y()/256,x1=(e.x()+e.width()-1)/256,y1=(e.y()+e.height()-1)/256,refs=(x1-x0+1)*(y1-y0+1);if(refs>64||count+refs>500000){large.add(entry);continue;}count+=refs;for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++)cells.computeIfAbsent(y*32+x,k->new ArrayList<>()).add(entry);}}
    Map<String,java.util.List<WorldDocument.Element>> query(Rectangle visible){Set<Integer> seen=new HashSet<>();var found=new ArrayList<Entry>();for(int y=Math.max(0,visible.y/256);y<=Math.min(31,(visible.y+visible.height)/256);y++)for(int x=Math.max(0,visible.x/256);x<=Math.min(31,(visible.x+visible.width)/256);x++)for(var entry:cells.getOrDefault(y*32+x,java.util.List.of()))include(entry,visible,seen,found);for(var entry:large)include(entry,visible,seen,found);found.sort(Comparator.comparingInt(Entry::order));Map<String,java.util.List<WorldDocument.Element>> result=new HashMap<>();for(var entry:found)result.computeIfAbsent(entry.element().layer(),k->new ArrayList<>()).add(entry.element());return result;}
    private void include(Entry entry,Rectangle r,Set<Integer> seen,java.util.List<Entry> found){var e=entry.element();if(e.visible()&&r.intersects(e.x(),e.y(),e.width(),e.height())&&seen.add(entry.order()))found.add(entry);}
}
