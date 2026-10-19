package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class QuestLifecycleTest {
    @Test void lifecycleAndPersistencePreventRepeatedCompletion(){var service=new QuestService(RpgContent.builtin());Map<String,Integer> p=new TreeMap<>();Set<String> f=new TreeSet<>();assertEquals(QuestService.State.UNAVAILABLE,service.state("sanctuary",p,f));assertTrue(service.accept("first_steps",p,f));assertFalse(service.complete("first_steps",p,f));service.defeat("slime",p,f);service.defeat("slime",p,f);assertTrue(service.complete("first_steps",p,f));assertFalse(service.complete("first_steps",p,f));assertEquals(QuestService.State.AVAILABLE,service.state("sanctuary",p,f));var w=new RpgWorld();w.interact();assertTrue(w.acceptQuest("first_steps"));assertEquals(w.save(),RpgWorld.restore(w.save()).save());}
}
