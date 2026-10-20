package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PortalGraphTest {
    @Test void validLinkRequiresExistingMapAndFreeArrival(){var a=WorldDocument.empty("origem",960,640).withPortals(List.of(new WorldDocument.Portal("porta",100,100,"destino",140,320,"","FRONT")));var b=WorldDocument.empty("destino",960,640);assertTrue(PortalGraph.validate(Map.of(a.id(),a,b.id(),b)).isEmpty());assertEquals(1,PortalGraph.validate(Map.of(a.id(),a)).size());var blocked=b.withHitboxes(List.of(new WorldDocument.Hitbox("wall",120,300,64,64,true,"collision")));assertEquals(1,PortalGraph.validate(Map.of(a.id(),a,b.id(),blocked)).size());}
    @Test void unknownQuestIsRejected(){var d=WorldDocument.empty("origem",960,640).withPortals(List.of(new WorldDocument.Portal("porta",100,100,"origem",140,320,"unknown","FRONT")));assertTrue(PortalGraph.validate(Map.of(d.id(),d)).get(0).description().contains("Missao"));}
}
