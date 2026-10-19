package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class QuestRewardsTest {
    @Test void stableEventsAndWorldRewardsAreIdempotent() {
        var service=new QuestService(RpgContent.builtin());Map<String,Integer> p=new TreeMap<>();Set<String> f=new TreeSet<>();service.accept("first_steps",p,f);
        service.event("kill","slime","forest_a",1,p,f);service.event("kill","slime","forest_a",1,p,f);assertEquals(1,p.get("first_steps"));service.event("kill","slime","forest_a",2,p,f);assertEquals(QuestService.State.READY,service.state("first_steps",p,f));
        var w=new RpgWorld();w.interact();w.acceptQuest("first_steps");var s=w.save();Map<String,Integer> progress=new TreeMap<>(s.quests());progress.put("first_steps",2);Set<String> flags=new TreeSet<>(s.flags());flags.add("qo:first_steps:0:2");
        w=RpgWorld.restore(new RpgSave(s.identity(),s.region(),s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),progress,flags,s.ticks()));w.interact();assertTrue(w.completeQuest("first_steps"));assertEquals(80,w.save().xp());assertEquals(45,w.save().coins());assertEquals(5,w.inventory().count("potion"));var before=w.save();assertFalse(w.completeQuest("first_steps"));assertEquals(before,w.save());
    }
}
