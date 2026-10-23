package com.projetok.studio;

import com.projetok.rpg.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Regressão conjunta de mapas, autoria, quatro raças e isolamento da campanha. */
class CorujaoFourRegressionTest {
    @TempDir Path folder;
    @Test void exportedMapAndAuthoredQuestRunForEveryRaceWithoutNewSaveFlags()throws Exception {
        var map=StudioExample.document();var catalog=new AssetCatalog.Result(List.of(),List.of(),List.of());var export=StudioExport.write(folder.resolve("bundle"),map.id(),Map.of(map.id(),map),catalog);var bundle=StudioExport.load(export);
        var quest=new QuestBook.Draft("visita","Visita à Aurora","Cumprimente a região.","elder","village","",1,"",true,List.of(),new QuestService.Definition(List.of(new QuestService.Objective("visit","village",1)),3,5,"potion",1),false);
        var book=new QuestBook(1,List.of(quest));QuestDocuments.save(folder.resolve("demo.sgquest"),book);book=QuestDocuments.load(folder.resolve("demo.sgquest"));
        for(var race:CharacterRaces.all()){
            var world=new RpgWorld(CharacterIdentity.create("Teste",race.id(),"azure"),bundle.scenes(),book);assertTrue(world.acceptAuthoredQuest("visita"));assertTrue(world.completeAuthoredQuest("visita"));var state=world.save();assertFalse(world.completeAuthoredQuest("visita"));assertEquals(state,world.save());assertEquals(3,state.xp());assertEquals(0,world.domainProgression().xpHalfUnits());assertTrue(state.flags().stream().noneMatch(f->f.contains("visita")));assertFalse(state.quests().containsKey("visita"));
        }
        assertEquals(0,new RpgWorld().save().xp());assertEquals(10,Domains.all().size());
    }
}
