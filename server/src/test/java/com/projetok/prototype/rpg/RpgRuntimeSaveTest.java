package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class RpgRuntimeSaveTest {
    @TempDir Path folder;
    @Test void closingCapturesQueuedCommandsBeforeIoAndPauses()throws Exception{
        try(var runtime=new RpgRuntime()) {var store=new LocalSaveStore(folder.resolve("save.properties"));runtime.command(w->w.inventory().add("crystal",4));var future=runtime.saveAndPause(store);runtime.advanceOneTick();future.get(3,TimeUnit.SECONDS);assertEquals(runtime.snapshot().state(),store.load());long tick=runtime.snapshot().ticks();runtime.advanceOneTick();assertEquals(tick,runtime.snapshot().ticks());}
    }
}
