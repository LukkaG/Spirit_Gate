package com.projetok.studio;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Valida persistência independente e limites de organização da biblioteca. */
class AssetLibraryPreferencesTest {
    @TempDir Path project;
    @Test void roundTripPreservesFavoritesRecentOrderAndSize()throws Exception {
        var p=AssetLibraryPreferences.empty().toggle("a".repeat(64)).used("b".repeat(64)).used("a".repeat(64)).size(96);p.save(project);
        assertEquals(p,AssetLibraryPreferences.load(project));assertEquals(2,p.used("a".repeat(64)).recent().size());
        assertTrue(p.toggle("a".repeat(64)).favorites().isEmpty());
    }
    @Test void recentIsBoundedAndInvalidReferencesAreRejected(){
        var p=AssetLibraryPreferences.empty();for(int i=0;i<50;i++)p=p.used(String.format("%064x",i));assertEquals(32,p.recent().size());
        assertThrows(IllegalArgumentException.class,()->AssetLibraryPreferences.empty().toggle("../../saves"));
        assertThrows(IllegalArgumentException.class,()->AssetLibraryPreferences.empty().size(10000));
    }
    @Test void unknownVersionDoesNotOverwriteOriginal()throws Exception {
        Path folder=Files.createDirectory(project.resolve("studio-projects")),file=folder.resolve("library.properties");Files.writeString(file,"schema=99\n");
        assertThrows(java.io.IOException.class,()->AssetLibraryPreferences.load(project));assertEquals("schema=99\n",Files.readString(file));
    }
}
