package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoHudTest {
    @Test void cooldownStaminaAndLowHpFollowWorldAndPersistedValues() {
        var w=new RpgWorld();var v=RpgCombatStatus.from(w.snapshot());assertEquals(1000,v.stamina());assertFalse(v.abilityReady());
        assertFalse(w.castAbility());assertFalse(w.castSecondaryAbility());
        w.awardExperience(RpgProgression.builtin().threshold(20));assertTrue(RpgCombatStatus.from(w.snapshot()).abilityReady());
        assertTrue(w.castSecondaryAbility());v=RpgCombatStatus.from(w.snapshot());assertEquals(750,v.stamina());assertEquals(90,v.abilityTicks());assertFalse(v.abilityReady());
        assertEquals("3.0 s",RpgCombatStatus.cooldown(v.abilityTicks()));
        assertEquals(v,RpgCombatStatus.from(RpgWorld.restore(w.save()).snapshot()));
        for(int tick=0;tick<6;tick++)w.tick(0,0);
        w.receiveDamage(w.maxHp()-1);assertTrue(RpgCombatStatus.from(w.snapshot()).lowHp());
        w.receiveDamage(10000);v=RpgCombatStatus.from(w.snapshot());assertEquals(0,v.hp());assertFalse(v.abilityReady());assertFalse(v.lowHp());
    }
    @Test void everyRaceUnlocksShunpoAtLevelTwentyRegardlessOfHealth() {
        for(var race:CharacterRaces.all()){
            var w=new RpgWorld(CharacterIdentity.create("Teste",race.id(),"azure"));assertFalse(RpgCombatStatus.from(w.snapshot()).abilityReady());
            w.awardExperience(RpgProgression.builtin().threshold(19));assertFalse(RpgCombatStatus.from(w.snapshot()).abilityReady());
            w.awardExperience(RpgProgression.builtin().threshold(20)-w.save().xp());assertTrue(RpgCombatStatus.from(w.snapshot()).abilityReady());
            w.receiveDamage(10);assertTrue(RpgCombatStatus.from(w.snapshot()).abilityReady());
        }
        assertEquals("Pronto",RpgCombatStatus.cooldown(-1));
    }
}
