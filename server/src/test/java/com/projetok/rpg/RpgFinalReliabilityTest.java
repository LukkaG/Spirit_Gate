package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RpgFinalReliabilityTest {
    @TempDir Path folder;
    @Test void preferencesAreSeparateAndPersist()throws Exception{var p=new RpgPreferences(23,true,false);Path file=folder.resolve("preferences.properties");p.save(file);assertEquals(p,RpgPreferences.load(file));assertThrows(IllegalArgumentException.class,()->new RpgPreferences(101,false,true));}
    @Test void malformedPendingRewardCannotOverwriteValidSave()throws Exception{var w=new RpgWorld();var store=new LocalSaveStore(folder.resolve("save.properties"));store.save(w.save());byte[] original=Files.readAllBytes(store.file());var s=w.save();var flags=new TreeSet<>(s.flags());flags.add("pending:crystal:bad");var invalid=new RpgSave(s.identity(),s.region(),s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),flags,s.ticks());assertThrows(IllegalArgumentException.class,()->store.save(invalid));assertArrayEquals(original,Files.readAllBytes(store.file()));}
    @Test void facingAndLootContinueDeterministically(){var w=EnemyRpgTest.forest();w.tick(-1,0);var copy=RpgWorld.restore(w.save());for(int i=0;i<100;i++){w.attack();copy.attack();w.tick(0,0);copy.tick(0,0);}assertEquals(w.save(),copy.save());}
    @Test void prolongedFixedSeedSimulationSurvivesRepeatedReloads()throws Exception{var w=new RpgWorld();var random=new Random(1966);for(int i=0;i<20000;i++){w.tick(random.nextInt(-1,2),random.nextInt(-1,2));if(i%90==0)w.castAbility();if(i%1000==0){var state=w.save();w=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(state)));assertEquals(state,w.save());}}assertEquals(20000,w.snapshot().ticks());}
}
