package com.projetok.rpg;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class SyntheticOnlineRepositoryTest {
    @TempDir Path temp;
    @Test void roundTripCasBackupIntegrityAndEphemeralAuthority()throws Exception{
        Path root=temp.resolve("online-synthetic-test");var repo=new SyntheticOnlineRepository(root);var grant=repo.create("spirit");
        Path file;try(var files=Files.list(root)){file=files.filter(p->p.toString().endsWith(".online")).findFirst().orElseThrow();}
        var id=java.util.UUID.fromString(file.getFileName().toString().replace(".online",""));var first=repo.load(id);
        assertEquals(1,first.revision());repo.write(grant,1,first.state());assertEquals(2,repo.load(id).revision());assertEquals(1,repo.recoverBackup(id).revision());
        assertThrows(java.io.IOException.class,()->repo.write(grant,1,first.state()));
        var restarted=new SyntheticOnlineRepository(root);assertEquals(first.state(),restarted.load(id).state());assertThrows(SecurityException.class,()->restarted.write(grant,2,first.state()));
        byte[] corrupt=Files.readAllBytes(file);corrupt[corrupt.length-1]^=1;Files.write(file,corrupt);assertThrows(java.io.IOException.class,()->repo.load(id));assertEquals(1,repo.recoverBackup(id).revision());
        assertThrows(IllegalArgumentException.class,()->new SyntheticOnlineRepository(temp.resolve("saves")));
    }
}
