package com.projetok.studio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DocumentStoreTest {
    @TempDir Path folder;
    @Test void roundTripAndBackupPreserveUserDataOnCorruption()throws Exception{var a=WorldDocument.empty("mapa",960,640).withMetadata(Map.of("teste","texto|:=,\nacentuado"));var p=folder.resolve("mapa.sgmap");DocumentStore.save(p,a);assertEquals(a,DocumentStore.load(p));var b=a.withElements(List.of(EditorOperationsTest.brush()));DocumentStore.save(p,b);assertEquals(a,DocumentStore.load(Path.of(p+".bak")));Files.writeString(p,"corrompido");byte[] backup=Files.readAllBytes(Path.of(p+".bak"));assertThrows(java.io.IOException.class,()->DocumentStore.save(p,b));assertEquals("corrompido",Files.readString(p));assertArrayEquals(backup,Files.readAllBytes(Path.of(p+".bak")));}
    @Test void unknownVersionsDuplicateKeysAndMalformedUtf8AreRejected()throws Exception{byte[] bytes=DocumentStore.encode(WorldDocument.empty("mapa",960,640));String text=new String(bytes,StandardCharsets.UTF_8);assertThrows(java.io.IOException.class,()->DocumentStore.decode((text+"\nschema=2\n").getBytes(StandardCharsets.UTF_8)));assertThrows(java.io.IOException.class,()->DocumentStore.decode(text.replace("schema=1","schema=9").getBytes(StandardCharsets.UTF_8)));assertThrows(java.io.IOException.class,()->DocumentStore.decode(new byte[]{(byte)0xc3,0x28}));}
}
