package com.projetok.rpg;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task195SessionOwnershipTest {
    @TempDir Path temp;
    @Test void wrongOwnerWrongIdentityDuplicateAndRevokedHandlesAreRejected()throws Exception {
        try(var lab=new SyntheticSessionLaboratory(temp.resolve("online-synthetic-owner"));
            var other=new SyntheticSessionLaboratory(temp.resolve("online-synthetic-other"))){
            var a=lab.create("spirit");var b=lab.create("human");var initial=lab.snapshot(a);
            assertThrows(IOException.class,()->lab.resume(initial.id()));
            assertThrows(SecurityException.class,()->other.snapshot(a));
            assertThrows(SecurityException.class,()->other.disconnect(a));
            assertThrows(SecurityException.class,()->lab.save(a,1,lab.snapshot(b).state()));
            assertThrows(SecurityException.class,()->lab.snapshot(null));
            assertEquals(initial,lab.snapshot(a));
            lab.disconnect(a);assertThrows(SecurityException.class,()->lab.snapshot(a));
            assertThrows(SecurityException.class,()->lab.save(a,1,initial.state()));
            var fresh=lab.resume(initial.id());assertEquals(initial,lab.snapshot(fresh));
            assertThrows(SecurityException.class,()->lab.disconnect(a));assertEquals(initial,lab.snapshot(fresh));
            assertThrows(IOException.class,()->lab.resume(UUID.randomUUID()));
        }
    }
    @Test void restartRequiresNewAuthorityAndPreservesConfirmedSnapshot()throws Exception {
        Path root=temp.resolve("online-synthetic-restart");var old=new SyntheticSessionLaboratory(root);
        var handle=old.create("spirit");var first=old.snapshot(handle);var confirmed=old.save(handle,1,first.state());old.close();
        assertThrows(IllegalStateException.class,()->old.snapshot(handle));
        try(var next=new SyntheticSessionLaboratory(root)){
            assertThrows(SecurityException.class,()->next.snapshot(handle));
            var resumed=next.resume(first.id());assertEquals(confirmed,next.snapshot(resumed));
            assertEquals(confirmed,next.save(resumed,1,first.state()));
            assertThrows(SecurityException.class,()->next.save(handle,2,first.state()));
        }
    }
    @Test void concurrentAcquisitionHasExactlyOneLocalOwner()throws Exception {
        try(var lab=new SyntheticSessionLaboratory(temp.resolve("online-synthetic-acquire"))){
            var original=lab.create("spirit");UUID id=lab.snapshot(original).id();lab.disconnect(original);
            var ready=new CountDownLatch(8);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(8);
            AtomicInteger acquired=new AtomicInteger(),rejected=new AtomicInteger();List<SyntheticSessionLaboratory.Handle> handles=Collections.synchronizedList(new ArrayList<>());
            try{
                List<Future<?>> work=new ArrayList<>();
                for(int i=0;i<8;i++)work.add(pool.submit(()->{ready.countDown();try{start.await();handles.add(lab.resume(id));acquired.incrementAndGet();}
                    catch(IOException expected){rejected.incrementAndGet();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}));
                assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();for(var result:work)result.get(5,TimeUnit.SECONDS);
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(3,TimeUnit.SECONDS));}
            assertEquals(1,acquired.get());assertEquals(7,rejected.get());assertEquals(id,lab.snapshot(handles.get(0)).id());
        }
    }
}
