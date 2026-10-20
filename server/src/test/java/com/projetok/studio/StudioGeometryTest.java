package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioGeometryTest {
    @Test void realCollisionAllowsTangencyAndIgnoresWalkableAnnotations(){var d=WorldDocument.empty("mapa",128,128).withHitboxes(List.of(new WorldDocument.Hitbox("solid",64,64,32,32,true,"collision")));assertTrue(StudioGeometry.free(d,46,80,18));assertFalse(StudioGeometry.free(d,47,80,18));assertTrue(StudioGeometry.free(d.withHitboxes(List.of(new WorldDocument.Hitbox("walk",64,64,32,32,false,"collision"))),80,80,18));}
    @Test void overlappingSolidsAreReportedNotModified(){var d=WorldDocument.empty("mapa",128,128).withHitboxes(List.of(new WorldDocument.Hitbox("a",32,32,32,32,true,"collision"),new WorldDocument.Hitbox("b",48,48,32,32,true,"collision")));assertEquals(1,StudioGeometry.overlaps(d).size());assertEquals(2,d.hitboxes().size());}
}
