package com.projetok.studio;

import com.projetok.rpg.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regiões autoradas são independentes da raça e respeitam os limites do save existente. */
class StudioRaceMapsTest {
    @Test void allFourRacesUseAuthoredMapAndSameGeometry(){
        var d=StudioIntegrationTest.map("mapa","village");var scenes=StudioExport.scenes(d.id(),Map.of(d.id(),d));
        for(var race:CharacterRaces.all()){var w=new RpgWorld(CharacterIdentity.create("Teste",race.id(),"azure"),scenes);for(int i=0;i<4;i++)w.tick(1,1);assertTrue(w.snapshot().x()>140);assertEquals("village",w.save().region());assertTrue(w.isSynthetic());}
    }
    @Test void largerEditorMapHasExplicitRuntimeLimitAndIsPreserved(){var d=WorldDocument.empty("grande",1920,1280);assertFalse(StudioRuntimeLimits.diagnostics(List.of(d)).isEmpty());assertThrows(IllegalArgumentException.class,()->StudioExport.scenes(d.id(),Map.of(d.id(),d)));assertEquals(1920,d.width());}
    @Test void spawnAtBoundaryIsRejectedByRealCollision(){var d=StudioIntegrationTest.map("mapa","village");var player=d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.PLAYER).findFirst().orElseThrow();var moved=new WorldDocument.Element(player.id(),player.kind(),player.asset(),player.sha(),player.crop(),0,0,player.width(),player.height(),player.layer(),player.visible(),player.locked(),player.reference(),player.direction(),player.properties());var elements=new ArrayList<>(d.elements());elements.set(elements.indexOf(player),moved);var invalid=d.withElements(elements);assertFalse(StudioGeometry.free(invalid,0,0,18));assertThrows(IllegalArgumentException.class,()->StudioExport.scenes(invalid.id(),Map.of(invalid.id(),invalid)));}
}
