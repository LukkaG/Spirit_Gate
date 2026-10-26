package com.projetok.rpg;

import java.nio.file.*;

/** Processo filho descartável: commit real seguido de temporário parcial e interrupção abrupta. */
public final class Task194CrashProbe {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var repo=new SyntheticOnlineRepository(root);var grant=repo.create("spirit");
        var id=Task194PersistenceTest.id(root,null);repo.write(grant,1,repo.load(id).state());
        Files.writeString(root.resolve("committed-id.txt"),id.toString());
        Files.write(root.resolve("snapshot-crash.tmp"),new byte[]{0x53,0x47,0x4f});
        Runtime.getRuntime().halt(23);
    }
}
