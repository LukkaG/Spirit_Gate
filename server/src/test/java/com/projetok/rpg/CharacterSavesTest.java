package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class CharacterSavesTest {
    @TempDir Path folder;
    @Test void listsCharactersAndRetainsCorruptFileWithRecoverableBackup() throws Exception {var repo=new CharacterSaves(folder);assertTrue(repo.list().isEmpty());var s=new RpgWorld().save();var file=repo.newFile(s.identity());var store=new LocalSaveStore(file);store.save(s);store.save(s);Files.writeString(file,"corrupt");var list=repo.list();assertEquals(2,list.size());assertNull(list.get(0).state());assertTrue(list.get(1).backup());assertEquals("corrupt",Files.readString(file));assertNotEquals(file,repo.recoveryFile(s.identity()));}
}
