package com.projetok.studio;
import com.projetok.rpg.*;
import com.projetok.prototype.rpg.RpgRuntime;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class StudioRuntimePlaytestTest {
    @Test void playtestAdvancesRealRuntimeAndClosesPendingActions()throws Exception{var d=StudioIntegrationTest.map("novo","village");var scenes=StudioExport.scenes(d.id(),Map.of(d.id(),d));try(var runtime=new RpgRuntime(new RpgWorld(CharacterIdentity.create("Teste","guardian","azure"),scenes))){runtime.input(1,0);runtime.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(runtime.snapshot().ticks()<4&&System.nanoTime()<deadline)Thread.sleep(5);assertTrue(runtime.snapshot().x()>140);assertNull(runtime.failure());var action=runtime.action(w->w.attack());runtime.close();assertNotNull(action.get(1,TimeUnit.SECONDS));}}
}
