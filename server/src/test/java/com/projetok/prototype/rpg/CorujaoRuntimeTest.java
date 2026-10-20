package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoRuntimeTest {
    @TempDir Path folder;
    @Test void pendingAfterPauseAndCloseCompletesAndSaveCapturesPriorCommands() throws Exception {
        try(var r=new RpgRuntime()) {
            var store=new LocalSaveStore(folder.resolve("save.properties"));r.command(w->w.inventory().add("crystal",2));
            var save=r.saveAndPause(store);var behind=r.action(w->true);r.advanceOneTick();save.get(3,TimeUnit.SECONDS);
            assertEquals(2,RpgWorld.restore(store.load()).inventory().count("crystal"));assertFalse(behind.get(1,TimeUnit.SECONDS));
            var action=r.action(w->true);r.close();assertFalse(action.get(1,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->r.save(store).get(1,TimeUnit.SECONDS));
        }
    }
    @Test void injectedTickFailureIsPublishedAndDrainsAllPendingFutures() throws Exception {
        var injected=new IllegalStateException("tick injetado");
        try(var r=new RpgRuntime(new RpgWorld(),()->{throw injected;})) {
            var action=r.action(w->true);var save=r.save(new LocalSaveStore(folder.resolve("failure.properties")));
            assertSame(injected,assertThrows(IllegalStateException.class,r::advanceOneTick));assertSame(injected,r.failure());
            assertFalse(action.get(1,TimeUnit.SECONDS));assertThrows(ExecutionException.class,()->save.get(1,TimeUnit.SECONDS));
            assertFalse(r.command(w->w.attack()));assertFalse(java.nio.file.Files.exists(folder.resolve("failure.properties")));
        }
    }
    @Test void concurrentActionsTickAndCloseNeverLeaveFuturesPending() throws Exception {
        var r=new RpgRuntime();var executor=Executors.newFixedThreadPool(3);
        var futures=new CopyOnWriteArrayList<CompletableFuture<Boolean>>();
        try {
            r.start();var submit=executor.submit(()->{for(int i=0;i<300;i++)futures.add(r.action(w->true));});
            var close=executor.submit(r::close);submit.get(3,TimeUnit.SECONDS);close.get(3,TimeUnit.SECONDS);
            for(var f:futures)assertNotNull(f.get(1,TimeUnit.SECONDS));assertFalse(r.command(w->w.attack()));
        } finally {r.close();executor.shutdownNow();}
    }
}
