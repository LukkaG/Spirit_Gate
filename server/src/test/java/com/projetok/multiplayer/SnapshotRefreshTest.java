package com.projetok.multiplayer;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotRefreshTest {
    @Test void aBurstQueuesOneRefreshAndReadsTheLatestState() {
        var queue=new ArrayDeque<Runnable>();int[] latest={0},display={-1};
        var signal=new SnapshotRefresh(queue::add,()->display[0]=latest[0]);
        for(int i=0;i<100;i++){latest[0]=i;signal.run();}
        assertEquals(1,queue.size());assertEquals(-1,display[0]);
        queue.remove().run();assertEquals(99,display[0]);
        latest[0]=100;signal.run();assertEquals(1,queue.size());
        queue.remove().run();assertEquals(100,display[0]);
    }
    @Test void aNewSnapshotDuringRefreshIsNotLost() {
        var queue=new ArrayDeque<Runnable>();int[] calls={0};SnapshotRefresh[] signal={null};
        signal[0]=new SnapshotRefresh(queue::add,()->{if(++calls[0]==1)signal[0].run();});
        signal[0].run();queue.remove().run();assertEquals(1,queue.size());
        queue.remove().run();assertEquals(2,calls[0]);
    }
    @Test void aRealReceivedSnapshotSignalsAfterPublishingItsState() throws Exception {
        try(var server=new LocalSimulationServer(java.net.InetAddress.getLoopbackAddress(),0)) {
            server.start();
            try(var client=new LocalClient(java.net.InetAddress.getLoopbackAddress(),server.port(),"Refresh")) {
                var arrived=new java.util.concurrent.CountDownLatch(1);
                var visible=new java.util.concurrent.atomic.AtomicReference<Protocol.State>();
                client.onSnapshot(()->{visible.set(client.snapshot());arrived.countDown();});
                assertTrue(arrived.await(3,java.util.concurrent.TimeUnit.SECONDS));
                assertNotNull(visible.get());
                assertTrue(visible.get().actors().stream().anyMatch(a->a.id()==client.id()));
            }
        }
    }
}
