package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Invariantes de movimento e aproximação com corpo físico, sem alterar balanceamento. */
class CorujaoMovementTest {
    @Test void diagonalsDoNotIncreaseMovementSpeedAndIdleDoesNotDrift(){var a=new RpgWorld();var b=new RpgWorld();for(int i=0;i<10;i++){a.tick(1,0);b.tick(1,1);}assertEquals(Math.hypot(a.snapshot().x()-140,a.snapshot().y()-320),Math.hypot(b.snapshot().x()-140,b.snapshot().y()-320),.001);float x=b.snapshot().x(),y=b.snapshot().y();for(int i=0;i<20;i++)b.tick(0,0);assertEquals(x,b.snapshot().x());assertEquals(y,b.snapshot().y());}
    @Test void offensiveShunpoCannotClipBodyThroughOffsetThinWall(){
        var space=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(170,330,1,20)),List.of());
        var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(space,new Position(100,320))),Map.of("probe","slime|forest|240|320"),Map.of(),Map.of(),Map.of());
        var w=new RpgWorld(CharacterIdentity.create("Teste","reaper","azure"),scenes);w.awardExperience(RpgProgression.builtin().threshold(20));var before=w.save();assertFalse(w.castAbility());assertEquals(before,w.save());
    }
    @Test void levelNineteenIsBlockedAndCooldownSurvivesRoundTrip()throws Exception {
        var w=new RpgWorld();w.awardExperience(RpgProgression.builtin().threshold(19));assertFalse(w.castSecondaryAbility());assertFalse(w.castAbility());w.awardExperience(RpgProgression.builtin().threshold(20)-w.save().xp());assertTrue(w.castSecondaryAbility());var saved=w.save();var loaded=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(saved)));assertFalse(loaded.castSecondaryAbility());assertFalse(loaded.castAbility());assertEquals(saved,loaded.save());
    }
}
