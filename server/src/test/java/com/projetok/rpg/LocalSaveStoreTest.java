package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalSaveStoreTest {
    @TempDir Path folder;
    @Test void roundTripAndFailurePreserveLastGoodSave() throws Exception {
        Path file=folder.resolve("save.properties"); LocalSaveStore store=new LocalSaveStore(file);
        assertThrows(NoSuchFileException.class,store::load);
        RpgWorld world=new RpgWorld(); world.tick(1,0); store.save(world.save());
        assertEquals(world.save(),store.load()); assertEquals(world.snapshot(),RpgWorld.restore(store.load()).snapshot());
        byte[] good=Files.readAllBytes(file);
        LocalSaveStore broken=new LocalSaveStore(file,(path,bytes)->{Files.write(path,new byte[]{1}); throw new IOException("disco cheio");});
        assertThrows(IOException.class,()->broken.save(world.save())); assertArrayEquals(good,Files.readAllBytes(file));
        store.save(world.save()); assertEquals(world.save(),store.recoverBackup());
    }
    @Test void rejectsCorruptionVersionAndDuplicateFields() throws Exception {
        byte[] good=LocalSaveStore.encode(new RpgWorld().save());
        String text=new String(good,java.nio.charset.StandardCharsets.ISO_8859_1);
        for(String invalid:new String[]{"garbage",text.replace("schema=1","schema=2"),text+"\nx=NaN\n",text.replace("x=140.0","x=NaN")})
            assertThrows(IllegalArgumentException.class,()->LocalSaveStore.decode(invalid.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)));
    }
}
