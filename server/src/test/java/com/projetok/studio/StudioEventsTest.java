package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioEventsTest {
    static WorldDocument.Trigger event(String id,String action,String target){return new WorldDocument.Trigger(id,"ENTER",action,target,"Mensagem",0,0,32,32,true);}
    @Test void whitelistCyclesOnceAndExecutionBudget(){var d=WorldDocument.empty("mapa",128,128).withTriggers(List.of(event("a","CHAIN","b"),event("b","CHAIN","a")));assertThrows(IllegalArgumentException.class,()->StudioEvents.validate(d));assertThrows(IllegalArgumentException.class,()->StudioEvents.validate(d.withTriggers(List.of(event("a","javascript","")))));var triggers=new ArrayList<WorldDocument.Trigger>();for(int i=0;i<20;i++)triggers.add(event("event_"+i,"MESSAGE",""));var valid=d.withTriggers(triggers);StudioEvents.validate(valid);var engine=new StudioEvents();var state=new StudioEvents.State("mapa",20,20,Set.of(),Set.of(),false);assertEquals(8,engine.update(valid,state).size());assertEquals(8,engine.update(valid,state).size());assertEquals(4,engine.update(valid,state).size());assertTrue(engine.update(valid,state).isEmpty());}
}
