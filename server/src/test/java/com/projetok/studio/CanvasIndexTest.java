package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.awt.Rectangle;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CanvasIndexTest {
    @Test void viewportMatchesBruteForceIncludingHugeOverlapsAndOrder(){var empty=WorldDocument.empty("mapa",8192,8192);var elements=new ArrayList<WorldDocument.Element>();var r=new Random(113);for(int i=0;i<1500;i++){int x=r.nextInt(7800),y=r.nextInt(7800);elements.add(new WorldDocument.Element("object_"+i,WorldDocument.Kind.OBJECT,"","",new WorldDocument.Crop(0,0,32,32),x,y,32,32,"ground",true,false,"","FRONT",Map.of()));}elements.add(new WorldDocument.Element("huge",WorldDocument.Kind.OBJECT,"","",new WorldDocument.Crop(0,0,32,32),0,0,8192,8192,"ground",true,false,"","FRONT",Map.of()));var d=empty.withElements(elements);var index=new CanvasIndex(d);for(int i=0;i<100;i++){var area=new Rectangle(r.nextInt(8192),r.nextInt(8192),1000,700);assertEquals(elements.stream().filter(e->area.intersects(e.x(),e.y(),e.width(),e.height())).toList(),index.query(area).getOrDefault("ground",List.of()));}}
}
