package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CorruptBackupProtectionTest {
    @TempDir Path folder;
    @Test void semanticallyCorruptCurrentFileCannotReplaceLastGoodBackup()throws Exception{
        var world=new RpgWorld();var state=world.save();var store=new LocalSaveStore(folder.resolve("save.properties"));store.save(state);store.save(state);
        var flags=new TreeSet<>(state.flags());flags.add("pending:crystal:bad");var bad=new RpgSave(state.identity(),state.region(),state.x(),state.y(),state.hp(),state.xp(),state.coins(),state.inventory(),state.equipment(),state.quests(),flags,state.ticks());Files.write(store.file(),LocalSaveStore.encode(bad));
        assertThrows(IOException.class,()->store.save(state));assertEquals(state,store.recoverBackup());
    }
}
