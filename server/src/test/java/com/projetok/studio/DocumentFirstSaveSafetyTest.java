package com.projetok.studio;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Otimização de temporários conserva arquivos existentes, backups e integridade. */
class DocumentFirstSaveSafetyTest {
    @TempDir Path folder;
    @Test void successiveSavesKeepPreviousValidDocumentAndUnrelatedTemporary()throws Exception {
        Path file=folder.resolve("mapa.sgmap"),unrelated=folder.resolve("studio-existing.tmp");Files.writeString(unrelated,"preservar");
        var first=WorldDocument.empty("mapa",960,640);DocumentStore.save(file,first);var second=first.resized(1920,1280);DocumentStore.save(file,second);
        assertEquals(second,DocumentStore.load(file));assertEquals(first,DocumentStore.load(Path.of(file+".bak")));assertEquals("preservar",Files.readString(unrelated));
        try(var files=Files.list(folder)){assertEquals(3,files.count());}
    }
}
