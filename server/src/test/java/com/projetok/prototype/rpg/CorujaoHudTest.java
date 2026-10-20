package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoHudTest {
    @Test void cooldownStaminaAndLowHpFollowWorldAndPersistedValues() {
        var w=new RpgWorld();var v=RpgCombatStatus.from(w.snapshot());assertEquals(1000,v.stamina());assertTrue(v.abilityReady());
        assertTrue(w.castAbility());v=RpgCombatStatus.from(w.snapshot());assertEquals(750,v.stamina());assertEquals(90,v.abilityTicks());assertFalse(v.abilityReady());
        assertEquals("3.0 s",RpgCombatStatus.cooldown(v.abilityTicks()));
        assertEquals(v,RpgCombatStatus.from(RpgWorld.restore(w.save()).snapshot()));
        w.receiveDamage(110);assertTrue(RpgCombatStatus.from(w.snapshot()).lowHp());
        w.receiveDamage(10000);v=RpgCombatStatus.from(w.snapshot());assertEquals(0,v.hp());assertFalse(v.abilityReady());assertFalse(v.lowHp());
    }
    @Test void mysticFullHealthIsUnavailableAndCooldownNeverNegative() {
        var w=new RpgWorld(CharacterIdentity.create("Teste","mystic","azure"));assertFalse(RpgCombatStatus.from(w.snapshot()).abilityReady());
        w.receiveDamage(10);assertTrue(RpgCombatStatus.from(w.snapshot()).abilityReady());assertEquals("Pronto",RpgCombatStatus.cooldown(-1));
    }
}
