package com.projetok.rpg;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Autoria precisa sobreviver ao roundtrip sem contornar regras do RPG. */
class QuestBookTest {
    @TempDir Path folder;
    static QuestBook.Draft draft(String id,String race,int level,String domain,List<String> previous){return new QuestBook.Draft(id,"Missão "+id,"Descrição\ncom acentos: ação","elder","village",race,level,domain,false,previous,new QuestService.Definition(List.of(new QuestService.Objective("visit","village",1)),3,5,"potion",1),false);}
    @Test void versionedRoundTripAndBackup()throws Exception {
        var book=new QuestBook(1,List.of(draft("teste","reaper",15,"water",List.of())));Path file=folder.resolve("test.sgquest");QuestDocuments.save(file,book);QuestDocuments.save(file,book);assertEquals(book,QuestDocuments.load(file));assertEquals(book,QuestDocuments.load(Path.of(file+".bak")));
    }
    @Test void duplicateUnknownAndExecutableFieldsAreRejected()throws Exception {
        byte[] bytes=QuestDocuments.encode(new QuestBook(1,List.of(draft("teste","",1,"",List.of()))));String text=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        assertThrows(java.io.IOException.class,()->QuestDocuments.decode((text+"\nscript=java.lang.Runtime\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(java.io.IOException.class,()->QuestDocuments.decode((text+"\nversion=1\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(java.io.IOException.class,()->QuestDocuments.decode(new byte[1_000_001]));
    }
    @Test void racialDomainLevelAndCyclesHaveDiagnostics(){
        assertFalse(new QuestBook(1,List.of(draft("a","human",15,"water",List.of()))).validate().isEmpty());
        assertFalse(new QuestBook(1,List.of(draft("a","reaper",14,"water",List.of()))).validate().isEmpty());
        assertTrue(new QuestBook(1,List.of(draft("a","",1,"",List.of("b")),draft("b","",1,"",List.of("a")))).validate().stream().anyMatch(s->s.contains("circular")));
        assertFalse(new QuestBook(1,List.of(draft("a","",1,"",List.of("missing")))).validate().isEmpty());
    }
    @Test void eligibilityUsesActualProgression(){
        var identity=CharacterIdentity.create("Sintético","reaper","azure");var q=draft("teste","reaper",15,"water",List.of());long xp=RpgProgression.builtin().threshold(15);
        assertFalse(q.eligible(identity,xp,DomainProgression.initial(),Set.of()));assertTrue(q.eligible(identity,xp,DomainProgression.initial().choose("reaper",15,"water"),Set.of()));
    }
}
