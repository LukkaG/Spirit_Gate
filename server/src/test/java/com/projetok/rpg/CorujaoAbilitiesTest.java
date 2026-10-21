package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoAbilitiesTest {
    static RpgWorld forest(String race,float x,int level) {
        var world=new RpgWorld(CharacterIdentity.create("Teste",race,"azure"));
        world.awardExperience(RpgProgression.builtin().threshold(level));var s=world.save();
        return RpgWorld.restore(new RpgSave(s.identity(),"forest",x,360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));
    }
    @Test void allRacesUnlockSharedShunpoAtExactlyLevel20WithoutDomain() throws Exception {
        for(var race:CharacterRaces.all()) {
            var locked=forest(race.id(),180,19);assertFalse(locked.castAbility());assertFalse(locked.castSecondaryAbility());
            var w=forest(race.id(),180,20);int enemyHp=w.snapshot().enemies().get(0).hp();
            assertTrue(w.castAbility());assertEquals(238,w.snapshot().x(),0.01);
            assertEquals(enemyHp,w.snapshot().enemies().get(0).hp());
            assertTrue(w.save().flags().contains("stamina:750"));
            assertFalse(w.castAbility());assertFalse(w.castSecondaryAbility());
            assertFalse(w.domainProgression().chosen());
            var restored=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(w.save())));
            assertEquals(w.save(),restored.save());assertFalse(restored.castAbility());assertFalse(restored.castSecondaryAbility());
        }
    }
    @Test void defensiveShunpoHasShortWindowAndResourcesPersist() throws Exception {
        var world=forest("human",180,20);float x=world.snapshot().x();int hp=world.save().hp();
        assertTrue(world.castSecondaryAbility());assertTrue(world.snapshot().x()<x);
        world.receiveDamage(50);assertEquals(hp,world.save().hp());
        var restored=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(world.save())));
        restored.receiveDamage(50);assertEquals(hp,restored.save().hp());
        for(int t=0;t<6;t++)restored.tick(0,0);
        restored.receiveDamage(50);assertEquals(hp-50,restored.save().hp());
        assertFalse(restored.castAbility());assertFalse(restored.castSecondaryAbility());
        var near=forest("reaper",180,20).save();var flags=new java.util.TreeSet<>(near.flags());
        flags.removeIf(f->f.startsWith("stamina:"));flags.add("stamina:100");
        var tired=RpgWorld.restore(new RpgSave(near.identity(),near.region(),near.x(),near.y(),near.hp(),near.xp(),near.coins(),near.inventory(),near.equipment(),near.quests(),flags,near.ticks()));
        assertFalse(tired.castAbility());assertFalse(tired.castSecondaryAbility());assertEquals(near.hp(),tired.save().hp());
        assertThrows(IllegalArgumentException.class,()->RpgSecondaryAbility.forArchetype("other"));
    }
    @Test void missingTargetAndBlockedDestinationsDoNotSpendResources() {
        var empty=new RpgWorld();empty.awardExperience(RpgProgression.builtin().threshold(20));
        assertFalse(empty.castAbility());assertTrue(empty.save().flags().contains("stamina:1000"));
        var dead=forest("reaper",180,20);dead.receiveDamage(10000);assertFalse(dead.castAbility());assertFalse(dead.castSecondaryAbility());
    }
}
