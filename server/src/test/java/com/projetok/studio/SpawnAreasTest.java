package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SpawnAreasTest {
    @Test void finiteAreaExpansionCollisionAndStableIds(){var e=new WorldDocument.Element("encounter",WorldDocument.Kind.ENEMY,"","",new WorldDocument.Crop(0,0,32,32),96,96,96,96,"interaction",true,false,"slime","FRONT",Map.of("spawn.quantity","4"));var d=WorldDocument.empty("mapa",256,256).withElements(List.of(e));assertEquals(4,SpawnAreas.points(d,e).size());assertNotEquals(SpawnAreas.key(d,e,0),SpawnAreas.key(d,e,1));var blocked=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",110,110,30,30,true,"collision")));assertThrows(IllegalArgumentException.class,()->SpawnAreas.points(blocked,e));assertEquals(d.elements(),blocked.elements());}
}
