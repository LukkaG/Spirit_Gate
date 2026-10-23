package com.projetok.rpg;

import java.util.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.projetok.prototype.rpg.RpgRuntime;
import static org.junit.jupiter.api.Assertions.*;

/** Cenários A–E de autoria executados no RpgWorld e no runtime reais. */
class AuthoredQuestRuntimeTest {
    @TempDir Path folder;
    QuestBook book(String race,int level,String domain){return new QuestBook(1,List.of(QuestBookTest.draft("autoria",race,level,domain,List.of())));}
    RpgWorld world(QuestBook book,String race){return new RpgWorld(CharacterIdentity.create("Sintético",race,"azure"),null,book);}
    @Test void scenarioAAndECompleteUnaffiliatedQuestExactlyOnce()throws Exception {
        var book=book("",1,"");Path file=folder.resolve("authored.sgquest");QuestDocuments.save(file,book);var w=world(QuestDocuments.load(file),"reaper");
        assertTrue(w.acceptAuthoredQuest("autoria"));assertEquals(QuestService.State.READY,w.authoredQuestViews().get(0).state());assertTrue(w.completeAuthoredQuest("autoria"));assertEquals(3,w.save().xp());assertEquals(0,w.domainProgression().xpHalfUnits());var before=w.save();assertFalse(w.completeAuthoredQuest("autoria"));assertEquals(before,w.save());assertFalse(w.save().quests().containsKey("autoria"));
    }
    @Test void scenariosBAndCRespectLevelAndChosenDomain(){
        var w=world(book("reaper",15,"water"),"reaper");assertFalse(w.acceptAuthoredQuest("autoria"));w.awardExperience(RpgProgression.builtin().threshold(15));assertFalse(w.acceptAuthoredQuest("autoria"));assertTrue(w.chooseDomain("water"));assertTrue(w.acceptAuthoredQuest("autoria"));assertTrue(w.completeAuthoredQuest("autoria"));assertEquals(3,w.domainProgression().xpHalfUnits());
    }
    @Test void scenarioDRejectsOtherRaceWithoutChangingCharacter(){var w=world(book("reaper",1,""),"human");var before=w.save();assertFalse(w.acceptAuthoredQuest("autoria"));assertEquals(before,w.save());}
    @Test void runtimeCannotPersistSyntheticCharacter()throws Exception {
        var w=world(book("",1,""),"spirit");Path path=folder.resolve("never.properties");try(var runtime=new RpgRuntime(w)){runtime.start();assertThrows(java.util.concurrent.ExecutionException.class,()->runtime.save(new LocalSaveStore(path)).get(5,TimeUnit.SECONDS));assertFalse(Files.exists(path));assertNull(runtime.failure());}
    }
}
