package com.projetok.rpg;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task196ResumeTest {
    @TempDir Path temp;
    void child(String mode,Path root)throws Exception {
        Path log=temp.resolve(mode+".log");
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx192m","-cp",
            System.getProperty("java.class.path"),Task196ResumeHarness.class.getName(),mode,root.toString())
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try{assertTrue(process.waitFor(20,TimeUnit.SECONDS));System.out.print(Files.readString(log));assertEquals(0,process.exitValue());}
        finally{if(process.isAlive())process.destroyForcibly();}
    }
    @Test void realSeparateProcessesResumeRewardsWithoutDuplication()throws Exception {
        Path root=temp.resolve("online-synthetic-two-process");child("seed",root);child("resume",root);
    }
    @Test void incompatibleContentRegionAndObstructedPositionFailSafely()throws Exception {
        Path root=temp.resolve("online-synthetic-content");
        try(var lab=new SyntheticSessionLaboratory(root)){
            var handle=lab.create("spirit");var original=lab.snapshot(handle);String hash=Task196ResumeHarness.contentFingerprint();
            assertThrows(IOException.class,()->Task196ResumeHarness.restoreConfirmed(original.state(),"0".repeat(64)));
            var s=original.state();var forest=new RpgSave(s.identity(),"forest",s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks());
            assertThrows(IOException.class,()->Task196ResumeHarness.restoreConfirmed(forest,hash));
            var obstructed=Task196ResumeHarness.positioned(s,400,200);
            assertThrows(IllegalArgumentException.class,()->Task196ResumeHarness.restoreConfirmed(obstructed,hash));
            assertThrows(IllegalArgumentException.class,()->lab.save(handle,1,obstructed));assertEquals(original,lab.snapshot(handle));
            assertEquals(original.state(),Task196ResumeHarness.restoreConfirmed(s,hash).save());
        }
    }
    @Test void divergentRetryAndStaleHandleNeverOverwriteConfirmedRewards()throws Exception {
        try(var lab=new SyntheticSessionLaboratory(temp.resolve("online-synthetic-negative"))){
            var handle=lab.create("spirit");var initial=lab.snapshot(handle);var state=Task196ResumeHarness.rewardedFixture(initial.state());
            var confirmed=lab.save(handle,1,state);lab.disconnect(handle);var fresh=lab.resume(initial.id());
            assertThrows(SecurityException.class,()->lab.save(handle,1,state));
            assertThrows(IOException.class,()->lab.save(fresh,1,initial.state()));
            assertEquals(confirmed,lab.snapshot(fresh));Task196ResumeHarness.assertNoReplay(confirmed.state());
        }
    }
}
