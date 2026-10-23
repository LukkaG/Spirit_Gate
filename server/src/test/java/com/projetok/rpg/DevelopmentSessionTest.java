package com.projetok.rpg;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checkpoint sintético preserva conclusão e não habilita controles em personagem comum. */
class DevelopmentSessionTest {
    @Test void checkpointRetainsAuthoredLedgerAndCannotDuplicateReward(){var book=new QuestBook(1,List.of(QuestBookTest.draft("demo","",1,"",List.of())));var w=new RpgWorld(CharacterIdentity.create("Teste","human","azure"),null,book);assertTrue(w.acceptAuthoredQuest("demo"));assertTrue(w.completeAuthoredQuest("demo"));var copy=w.forkSynthetic();assertEquals(w.save(),copy.save());assertEquals(w.authoredQuestViews(),copy.authoredQuestViews());assertFalse(copy.completeAuthoredQuest("demo"));assertEquals(w.save(),copy.save());assertThrows(IllegalStateException.class,()->new RpgWorld().forkSynthetic());}
    @Test void diagnosticsShowRealRequirementsAndExactMastery(){var w=new RpgWorld();String text=String.join("\n",RpgDevelopmentDiagnostics.lines(w.snapshot(),"padrão"));assertTrue(text.contains("nível 20+"));assertTrue(text.contains("não escolhido"));assertTrue(text.contains("meias unidades: 0"));}
}
