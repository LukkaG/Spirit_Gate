package com.projetok.rpg;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Fronteira entre pacotes: observação de controles e owner em laboratório, sem prova TCP artificial. */
class RpgCharacterObservationTest {
    @Test void rootStunSlowAndUnknownExternalEffectsArePreservedExactly() {
        var world=new RpgWorld(CharacterIdentity.create("Controles Ponte","reaper","azure"),
                new RpgSceneSet("village",Map.of("village",new RpgSceneSet.Geometry(
                        new com.projetok.domain.movement.CollisionService.Space(960,640,List.of(),List.of()),
                        new com.projetok.domain.entity.Position(140,320))),Map.of(),Map.of(),Map.of(),Map.of()));
        world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT,37,0,0,0));
        world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.STUN,29,0,0,0));
        world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.SLOW,41,600,0,0));
        var engine=new RpgSkillEngine();
        var before=RpgCharacterObservation.read(Thread.currentThread(),1,world,engine,Map.of(1,world));
        var controls=before.character().restrictions();assertEquals(37,controls.rootUntil());
        assertEquals(29,controls.stunUntil());assertEquals(41,controls.slowUntil());assertEquals(.6f,controls.slowScale());
        assertFalse(controls.externalEffectsKnown());
        assertEquals(before,RpgCharacterObservation.read(Thread.currentThread(),1,world,engine,Map.of(1,world)));
        assertTrue(world.skillMovementBlocked());assertTrue(world.skillActionsBlocked());
    }

    @Test void wrongOwnerIsRejectedBeforeNullWorldIsRead() throws Exception {
        var owner=Thread.currentThread();var executor=Executors.newSingleThreadExecutor();
        try{executor.submit(()->assertThrows(IllegalStateException.class,
                ()->RpgCharacterObservation.read(owner,1,null,null,null))).get(5,TimeUnit.SECONDS);}
        finally{executor.shutdownNow();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}
    }

    @Test void participantMismatchCannotObserveAnotherWorld() {
        var world=new RpgWorld();var engine=new RpgSkillEngine();var before=world.networkState();
        assertThrows(IllegalArgumentException.class,()->RpgCharacterObservation.read(Thread.currentThread(),1,
                world,engine,Map.of(2,world)));assertEquals(before,world.networkState());
    }
}
