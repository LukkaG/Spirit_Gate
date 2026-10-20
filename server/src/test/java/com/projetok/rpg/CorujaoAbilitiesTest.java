package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoAbilitiesTest {
    static RpgWorld forest(String archetype,float x) {
        var s=new RpgWorld(CharacterIdentity.create("Teste",archetype,"azure")).save();
        return RpgWorld.restore(new RpgSave(s.identity(),"forest",x,360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));
    }
    @Test void bothAdditionalAbilitiesHaveExactCostSingleDamageAndPersistedCooldown() throws Exception {
        for(String archetype:new String[]{"guardian","mystic"}) {
            var w=forest(archetype,240);var ability=RpgSecondaryAbility.forArchetype(archetype);
            int hp=w.snapshot().enemies().get(0).hp();assertTrue(w.castSecondaryAbility());
            assertEquals(hp-w.attackPower()-ability.bonus(),w.snapshot().enemies().get(0).hp());
            assertTrue(w.save().flags().contains("stamina:"+(1000-ability.cost()*10)));
            assertFalse(w.castSecondaryAbility());assertFalse(w.castAbility());assertFalse(w.attack());
            var restored=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(w.save())));
            assertEquals(w.save(),restored.save());assertFalse(restored.castSecondaryAbility());
            assertEquals(1,w.snapshot().events().stream().filter(e->e.kind()==RpgCombatEvent.Kind.HIT).count());
        }
    }
    @Test void missesAndInsufficientResourcesDoNotConsumeAndRangesDiffer() {
        var guardian=forest("guardian",180);assertFalse(guardian.castSecondaryAbility());
        assertTrue(forest("mystic",180).castSecondaryAbility());
        var s=guardian.save();assertTrue(s.flags().contains("stamina:1000"));
        guardian.receiveDamage(10000);assertFalse(guardian.castSecondaryAbility());
        var near=forest("guardian",240).save();var flags=new java.util.TreeSet<>(near.flags());
        flags.removeIf(f->f.startsWith("stamina:"));flags.add("stamina:100");
        var tired=RpgWorld.restore(new RpgSave(near.identity(),near.region(),near.x(),near.y(),near.hp(),near.xp(),near.coins(),near.inventory(),near.equipment(),near.quests(),flags,near.ticks()));
        assertFalse(tired.castSecondaryAbility());assertEquals(near.hp(),tired.save().hp());
        assertThrows(IllegalArgumentException.class,()->RpgSecondaryAbility.forArchetype("other"));
    }
}
