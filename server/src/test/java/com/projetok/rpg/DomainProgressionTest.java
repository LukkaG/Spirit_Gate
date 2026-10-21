package com.projetok.rpg;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainProgressionTest {
    @Test void choiceRequiresInitiationIsRacialAndCannotBeSwapped() {
        for(var definition:Domains.all()) {
            var empty=DomainProgression.initial();
            assertThrows(IllegalStateException.class,()->empty.choose(definition.race(),14,definition.id()));
            var chosen=empty.choose(definition.race(),15,definition.id());
            assertEquals(definition.id(),chosen.domain());assertEquals(0,chosen.xpHalfUnits());
            assertThrows(IllegalStateException.class,()->chosen.choose(definition.race(),30,definition.id()));
            String wrongRace=definition.race().equals("spirit")?"human":"spirit";
            assertThrows(IllegalArgumentException.class,()->empty.choose(wrongRace,15,definition.id()));
        }
    }
    @Test void halfExperienceIsExactDoesNotAccumulateBeforeChoiceAndSurvivesPersistence() throws Exception {
        var world=new RpgWorld();world.awardExperience(RpgProgression.builtin().threshold(15));
        assertEquals(0,world.domainProgression().xpHalfUnits());assertTrue(world.chooseDomain("water"));
        long xp=world.save().xp();world.awardExperience(3);
        assertEquals(xp+3,world.save().xp());assertEquals(3,world.domainProgression().xpHalfUnits());
        assertEquals(1.5,world.domainProgression().xp());assertEquals("1,5",world.domainProgression().xpText());
        var loaded=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(world.save())));
        assertEquals(world.save(),loaded.save());assertEquals(3,loaded.domainProgression().xpHalfUnits());
        loaded.awardExperience(3);assertEquals(6,loaded.domainProgression().xpHalfUnits());
        assertFalse(loaded.chooseDomain("thunder"));assertEquals("water",loaded.domainProgression().domain());
    }
    @Test void killingLevelOneMobAwardsThreeNormalAndOneAndHalfDomainXpOnlyOnce() {
        var world=EnemyRpgTest.forest();world.awardExperience(RpgProgression.builtin().threshold(15));
        assertTrue(world.chooseDomain("water"));long previous=world.save().xp();
        assertTrue(world.attack());assertEquals(previous+3,world.save().xp());
        assertEquals(3,world.domainProgression().xpHalfUnits());
        for(int t=0;t<12;t++)world.tick(0,0);
        assertFalse(world.attack());assertEquals(previous+3,world.save().xp());
        assertEquals(3,world.domainProgression().xpHalfUnits());
    }
    @Test void resetCosts35NeedsQuestAndWaitsOneCalendarMonth() {
        var policy=DomainResetPolicy.builtin();assertEquals(35,policy.cashCost());
        var chosen=DomainProgression.initial().choose("reaper",15,"water").award(3);
        var date=LocalDate.of(2028,1,31);
        assertThrows(IllegalStateException.class,()->chosen.reset(date,35,policy));
        var ready=chosen.completeResetQuest();
        assertThrows(IllegalStateException.class,()->ready.reset(date,34,policy));
        assertThrows(IllegalStateException.class,()->ready.reset(date,100,DomainResetPolicy.disabled()));
        var reset=ready.reset(date,35,policy);assertFalse(reset.chosen());assertEquals(0,reset.xpHalfUnits());
        assertFalse(reset.resetQuestComplete());assertEquals(LocalDate.of(2028,2,29),reset.nextResetDate());
        var next=reset.choose("reaper",15,"fire").completeResetQuest();
        assertThrows(IllegalStateException.class,()->next.reset(LocalDate.of(2028,2,28),35,policy));
        assertFalse(next.reset(LocalDate.of(2028,2,29),35,policy).chosen());
    }
    @Test void resetClearsOnlyDomainSkillsAndPreservesBaseCharacterAndEquipment() {
        var world=new RpgWorld();world.awardExperience(RpgProgression.builtin().threshold(20));
        assertTrue(world.chooseDomain("water"));world.awardExperience(3);var before=world.save();
        var flags=new TreeSet<>(before.flags());flags.add("domain-skill:water_aparo");
        var reset=world.domainProgression().completeResetQuest().reset(LocalDate.of(2026,10,21),35,new DomainResetPolicy(35));
        reset.writeTo(flags);assertFalse(flags.contains("domain-skill:water_aparo"));
        var after=new RpgSave(before.identity(),before.region(),before.x(),before.y(),before.hp(),before.xp(),before.coins(),before.inventory(),before.equipment(),before.quests(),flags,before.ticks());
        var restored=RpgWorld.restore(after);
        assertEquals(before.identity(),restored.save().identity());assertEquals(before.xp(),restored.save().xp());
        assertEquals(before.inventory(),restored.save().inventory());assertEquals(before.equipment(),restored.save().equipment());
        assertTrue(restored.canShunpo());assertFalse(restored.domainProgression().chosen());
    }
    @Test void corruptOrConflictingDomainStateIsRejected() {
        for(Set<String> flags:java.util.List.of(Set.of("domain:fire","domain:water"),Set.of("domain:none","mastery-half:1"),Set.of("domain:unknown"),Set.of("mastery-half:-1"),Set.of("reset-last:2026-99-99"),Set.of("reset-quest:no")))
            assertThrows(IllegalArgumentException.class,()->DomainProgression.read(flags));
        var world=new RpgWorld();var before=world.save();
        var flags=new TreeSet<>(before.flags());new DomainProgression("water",0,null,false).writeTo(flags);
        assertThrows(IllegalArgumentException.class,()->new RpgSave(before.identity(),before.region(),before.x(),before.y(),before.hp(),before.xp(),before.coins(),before.inventory(),before.equipment(),before.quests(),flags,before.ticks()));
    }
    @Test void masteryOverflowCannotPartiallyAwardCharacterExperience() {
        var world=new RpgWorld();world.awardExperience(RpgProgression.builtin().threshold(15));
        assertTrue(world.chooseDomain("water"));var save=world.save();
        var flags=new TreeSet<>(save.flags());new DomainProgression("water",Long.MAX_VALUE,null,false).writeTo(flags);
        var restored=RpgWorld.restore(new RpgSave(save.identity(),save.region(),save.x(),save.y(),save.hp(),save.xp(),save.coins(),save.inventory(),save.equipment(),save.quests(),flags,save.ticks()));
        var before=restored.save();
        assertThrows(ArithmeticException.class,()->restored.awardExperience(3));
        assertEquals(before,restored.save());
    }
}
