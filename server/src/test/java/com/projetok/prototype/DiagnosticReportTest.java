package com.projetok.prototype;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticReportTest {
    @TempDir Path temp;
    @Test void reportUsesWhitelistAndExcludesPrivateMessagesAndProperties() throws Exception {
        var base=DebugWorld.demo().snapshot();
        var components=DebugSnapshot.class.getRecordComponents();
        Object[] values=new Object[components.length];Class<?>[] types=new Class<?>[components.length];
        for(int i=0;i<components.length;i++) {
            values[i]=components[i].getAccessor().invoke(base);types[i]=components[i].getType();
            if(components[i].getName().equals("lastEvent"))values[i]="secret-token C:\\Users\\PrivateName\\private.key";
        }
        var snapshot=DebugSnapshot.class.getDeclaredConstructor(types).newInstance(values);
        String text=DiagnosticReport.json(snapshot,1260,750,true,DiagnosticReport.Stage.ARENA,
                new RuntimeException("password=private-secret /home/PrivateName"));
        for(String forbidden:new String[]{"PrivateName","password","secret-token","private.key","user.home","user.name","C:\\Users","/home/"})
            assertFalse(text.contains(forbidden),forbidden);
        assertTrue(text.contains("LOCAL_MAP"));assertTrue(text.contains("REPORTED_ERROR_DETAILS_OMITTED"));
        assertTrue(text.contains("\"hp\": "+base.playerHp()));
    }
    @Test void exportIsExplicitSingleEntryAndNeverOverwrites() throws Exception {
        var snapshot=DebugWorld.demo().snapshot();Path zip=temp.resolve("report.zip");
        DiagnosticReport.export(zip,snapshot,800,600,false,DiagnosticReport.Stage.ARENA,null);
        try(var archive=new ZipFile(zip.toFile())) {
            assertEquals(1,archive.size());var entry=archive.getEntry("diagnostico.json");assertNotNull(entry);
            assertTrue(new String(archive.getInputStream(entry).readAllBytes(),StandardCharsets.UTF_8).contains("DEFAULT_ARENA"));
        }
        assertThrows(java.nio.file.FileAlreadyExistsException.class,()->DiagnosticReport.export(zip,snapshot,800,600,false,DiagnosticReport.Stage.ARENA,null));
        assertThrows(IllegalArgumentException.class,()->DiagnosticReport.json(snapshot,0,600,false,DiagnosticReport.Stage.ARENA,null));
    }
}
