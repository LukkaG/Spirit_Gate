package com.projetok.rpg;

import java.util.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Rejeição precisa preservar todos os recursos, inclusive quando há overflow de domínio. */
class QuestRewardsTest {
    final InventoryService inventory=new InventoryService(new ItemCatalog(RpgContent.builtin()),20);
    final QuestService.Definition reward=new QuestService.Definition(List.of(new QuestService.Objective("visit","village",1)),3,5,"potion",1);
    @Test void exactHalfUnitsOnlyAfterChoice(){
        long xp=RpgProgression.builtin().threshold(15);var before=QuestRewards.prepare(inventory,0,xp,DomainProgression.initial(),reward);assertEquals(0,before.domain().xpHalfUnits());
        var after=QuestRewards.prepare(inventory,0,xp,DomainProgression.initial().choose("reaper",15,"water"),reward);assertEquals(3,after.domain().xpHalfUnits());assertEquals("1,5",after.domain().xpText());assertEquals(0,inventory.count("potion"));
    }
    @Test void overflowAndInvalidRewardPreserveInventory(){
        inventory.add("crystal",2);var snapshot=inventory.snapshot();var domain=new DomainProgression("water",Long.MAX_VALUE,null,false);
        var collect=new QuestService.Definition(List.of(new QuestService.Objective("collect","crystal",2)),3,5,"potion",1);
        assertThrows(ArithmeticException.class,()->QuestRewards.prepare(inventory,0,10000,domain,collect));assertEquals(snapshot,inventory.snapshot());
        assertThrows(IllegalStateException.class,()->QuestRewards.prepare(inventory,1_000_000,0,DomainProgression.initial(),reward));assertEquals(snapshot,inventory.snapshot());
        assertThrows(IllegalArgumentException.class,()->QuestRewards.prepare(inventory,0,0,DomainProgression.initial(),new QuestService.Definition(reward.objectives(),-1,0,"potion",1)));
    }
    @Test void resetContractCannotGrantResetByVisualConfiguration(){
        var domain=DomainProgression.initial().choose("reaper",15,"water");var today=LocalDate.of(2026,10,22);
        assertThrows(IllegalStateException.class,()->domain.reset(today,35,DomainResetPolicy.builtin()));
        assertThrows(IllegalStateException.class,()->domain.completeResetQuest().reset(today,34,DomainResetPolicy.builtin()));
        var reset=domain.completeResetQuest().reset(today,35,DomainResetPolicy.builtin());assertFalse(reset.chosen());assertEquals(today,reset.lastReset());assertTrue(domain.chosen());
        var again=reset.choose("reaper",15,"water").completeResetQuest();assertThrows(IllegalStateException.class,()->again.reset(today.plusDays(1),35,DomainResetPolicy.builtin()));
    }
    @Test void stableEventsAndWorldRewardsAreIdempotent() {
        var service=new QuestService(RpgContent.builtin());Map<String,Integer> p=new TreeMap<>();Set<String> f=new TreeSet<>();service.accept("first_steps",p,f);
        service.event("kill","slime","forest_a",1,p,f);service.event("kill","slime","forest_a",1,p,f);assertEquals(1,p.get("first_steps"));service.event("kill","slime","forest_a",2,p,f);assertEquals(QuestService.State.READY,service.state("first_steps",p,f));
        var w=new RpgWorld();w.interact();w.acceptQuest("first_steps");var s=w.save();Map<String,Integer> progress=new TreeMap<>(s.quests());progress.put("first_steps",2);Set<String> flags=new TreeSet<>(s.flags());flags.add("qo:first_steps:0:2");
        w=RpgWorld.restore(new RpgSave(s.identity(),s.region(),s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),progress,flags,s.ticks()));w.interact();assertTrue(w.completeQuest("first_steps"));assertEquals(80,w.save().xp());assertEquals(45,w.save().coins());assertEquals(5,w.inventory().count("potion"));var before=w.save();assertFalse(w.completeQuest("first_steps"));assertEquals(before,w.save());
    }
}
