package com.projetok.rpg;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task194PersistenceTest {
    @TempDir Path temp;
    static UUID id(Path root, UUID except)throws Exception {
        try(var files=Files.list(root)){return files.filter(p->p.toString().endsWith(".online"))
            .map(p->UUID.fromString(p.getFileName().toString().replace(".online","")))
            .filter(value->!value.equals(except)).findFirst().orElseThrow();}
    }
    @Test void backupMustMatchRequestedIdentity()throws Exception {
        Path root=temp.resolve("online-synthetic-backup");var repo=new SyntheticOnlineRepository(root);
        var grant=repo.create("spirit");UUID a=id(root,null);var original=repo.load(a);
        repo.write(grant,1,original.state());repo.create("human");UUID b=id(root,a);
        Files.copy(root.resolve(b+".online"),root.resolve(a+".online.bak"),StandardCopyOption.REPLACE_EXISTING);
        assertThrows(IOException.class,()->repo.recoverBackup(a));
        assertEquals(original.state(),repo.load(a).state());assertEquals(2,repo.load(a).revision());
    }
    @Test void overlappingLockIsCheckedFailureAndPrimaryIsPreserved()throws Exception {
        Path root=temp.resolve("online-synthetic-lock");var repo=new SyntheticOnlineRepository(root);
        var grant=repo.create("spirit");UUID id=id(root,null);var first=repo.load(id);
        byte[] before=Files.readAllBytes(root.resolve(id+".online"));
        try(var channel=FileChannel.open(root.resolve(id+".lock"),StandardOpenOption.WRITE);var held=channel.lock()){
            assertThrows(IOException.class,()->repo.write(grant,1,first.state()));
        }
        assertArrayEquals(before,Files.readAllBytes(root.resolve(id+".online")));
        repo.write(grant,1,first.state());assertEquals(2,repo.load(id).revision());
    }
    @Test void concurrentCasHasExactlyOneCommit()throws Exception {
        for(int round=0;round<12;round++){
            Path root=temp.resolve("online-synthetic-race-"+round);var repo=new SyntheticOnlineRepository(root);
            var grant=repo.create("spirit");UUID id=id(root,null);var state=repo.load(id).state();
            var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var workers=Executors.newFixedThreadPool(2);
            AtomicInteger committed=new AtomicInteger(),conflicts=new AtomicInteger();
            try{
                List<Future<?>> results=new ArrayList<>();
                for(int i=0;i<2;i++)results.add(workers.submit(()->{ready.countDown();try{start.await();repo.write(grant,1,state);committed.incrementAndGet();}
                    catch(IOException expected){conflicts.incrementAndGet();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}));
                assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();for(var result:results)result.get(5,TimeUnit.SECONDS);
            }finally{workers.shutdownNow();assertTrue(workers.awaitTermination(3,TimeUnit.SECONDS));}
            assertEquals(1,committed.get());assertEquals(1,conflicts.get());assertEquals(2,repo.load(id).revision());
            assertEquals(state,repo.load(id).state());assertEquals(1,repo.recoverBackup(id).revision());
        }
    }
    @Test void corruptionAndOrphanTemporaryNeverBecomeConfirmed()throws Exception {
        Path root=temp.resolve("online-synthetic-corrupt");var repo=new SyntheticOnlineRepository(root);
        var grant=repo.create("spirit");UUID id=id(root,null);var state=repo.load(id).state();repo.write(grant,1,state);
        Files.write(root.resolve("snapshot-interrupted.tmp"),new byte[]{1,2,3});
        var restarted=new SyntheticOnlineRepository(root);assertEquals(2,restarted.load(id).revision());
        byte[] bytes=Files.readAllBytes(root.resolve(id+".online"));bytes[bytes.length-1]^=1;Files.write(root.resolve(id+".online"),bytes);
        assertThrows(IOException.class,()->restarted.load(id));assertEquals(state,restarted.recoverBackup(id).state());
        assertThrows(SecurityException.class,()->restarted.write(grant,2,state));
        assertThrows(IOException.class,()->repo.write(grant,2,state));
    }
    @Test void retryOnlyReconcilesEntireIdenticalSnapshotAtNextRevision()throws Exception {
        Path root=temp.resolve("online-synthetic-retry");var repo=new SyntheticOnlineRepository(root);
        var grant=repo.create("spirit");UUID id=id(root,null);var state=repo.load(id).state();
        var committed=repo.writeIdenticalRetry(grant,1,state);assertEquals(2,committed.revision());
        byte[] primary=Files.readAllBytes(root.resolve(id+".online")),backup=Files.readAllBytes(root.resolve(id+".online.bak"));
        for(int i=0;i<20;i++)assertEquals(committed,repo.writeIdenticalRetry(grant,1,state));
        assertArrayEquals(primary,Files.readAllBytes(root.resolve(id+".online")));
        assertArrayEquals(backup,Files.readAllBytes(root.resolve(id+".online.bak")));
        var changed=new RpgSave(state.identity(),state.region(),state.x(),state.y(),state.hp(),state.xp(),state.coins()+1,
            state.inventory(),state.equipment(),state.quests(),state.flags(),state.ticks());
        assertThrows(IOException.class,()->repo.writeIdenticalRetry(grant,1,changed));
        assertThrows(IOException.class,()->repo.writeIdenticalRetry(grant,0,state));
        assertThrows(IOException.class,()->repo.write(grant,1,state));
        repo.create("human");var other=repo.load(id(root,id));
        assertThrows(SecurityException.class,()->repo.writeIdenticalRetry(grant,1,other.state()));
        assertEquals(committed,repo.load(id));
    }
    @Test void abruptChildExitPreservesConfirmedPrimaryAndBackup()throws Exception {
        Path root=temp.resolve("online-synthetic-process");
        String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
        var process=new ProcessBuilder(java,"-Xmx128m","-cp",System.getProperty("java.class.path"),
            Task194CrashProbe.class.getName(),root.toString()).redirectErrorStream(true).redirectOutput(temp.resolve("child.log").toFile()).start();
        try{assertTrue(process.waitFor(15,TimeUnit.SECONDS));assertEquals(23,process.exitValue());}
        finally{if(process.isAlive())process.destroyForcibly();}
        UUID id=UUID.fromString(Files.readString(root.resolve("committed-id.txt")));
        var restarted=new SyntheticOnlineRepository(root);assertEquals(2,restarted.load(id).revision());
        assertEquals(1,restarted.recoverBackup(id).revision());assertEquals(restarted.load(id).state(),restarted.recoverBackup(id).state());
        assertEquals(3,Files.size(root.resolve("snapshot-crash.tmp")));
    }
}
