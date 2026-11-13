package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A leitura de rede conserva valores de jogo sem produzir um save completo. */
class NetworkStateTest {
    private static RpgWorld world() {
        var space=new CollisionService.Space(960,640,List.of(),List.of());
        var scenes=new RpgSceneSet("forest",Map.of(
                "forest",new RpgSceneSet.Geometry(space,new Position(140,300)),
                "village",new RpgSceneSet.Geometry(space,new Position(140,320))),
                Map.of(),Map.of("elder","village|190|320"),Map.of(),Map.of("to_village","forest|140|300|village|140|320"));
        var world=new RpgWorld(CharacterIdentity.create("Viajante","reaper","azure"),scenes);
        world.useNetworkFootprint();
        return world;
    }
    private static void equivalent(RpgWorld world) {
        var state=world.networkState();var full=world.snapshot();
        assertAll(()->assertEquals(full.x(),state.x()),()->assertEquals(full.y(),state.y()),
                ()->assertEquals(full.ticks(),state.ticks()),()->assertEquals(full.identity(),state.identity()),
                ()->assertEquals(full.state().region(),state.region()),()->assertEquals(full.state().hp(),state.hp()),
                ()->assertEquals(full.maxHp(),state.maxHp()),()->assertEquals(full.state().coins(),state.coins()),
                ()->assertTrue(full.state().flags().contains("stamina:"+state.stamina())),
                ()->assertEquals(full.npcs(),world.networkNpcs()),()->assertEquals(full.portals(),world.networkPortals()));
    }
    @Test void matchesFullStateAfterMovementDamagePortalProgressionAndRespawn() {
        var world=world();equivalent(world);
        world.tick(1,0);equivalent(world);
        assertTrue(world.travel("to_village"));equivalent(world);
        assertEquals("village",world.networkState().region());
        assertEquals(1,world.networkNpcs().size());
        assertThrows(UnsupportedOperationException.class,()->world.networkNpcs().clear());
        world.awardExperience(RpgProgression.builtin().threshold(20));equivalent(world);
        assertTrue(world.castSecondaryAbility());equivalent(world);
        assertTrue(world.networkState().stamina()<1000);
        for(int i=0;i<7;i++)world.tick(0,0);
        world.receiveDamage(7);equivalent(world);
        world.receiveDamage(10000);equivalent(world);
        assertEquals(0,world.networkState().hp());
        assertTrue(world.returnHome());equivalent(world);
        assertEquals("forest",world.networkState().region());
    }
    @Test void repeatedReadsDoNotChangePersistentStateAndReturnedValueIsStable() {
        var world=world();var before=world.save();var captured=world.networkState();
        for(int i=0;i<1000;i++)assertEquals(captured,world.networkState());
        assertEquals(before,world.save());
        world.tick(1,0);
        assertEquals(140,captured.x());assertEquals(0,captured.ticks());
        assertNotEquals(captured,world.networkState());
    }
}
