package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
/** Fluxo real: nenhuma injecao de XP, moeda, item, posicao ou conclusao de quest. */
class OfflineCycleTest {
    @TempDir Path folder;
    static void move(RpgWorld w,float x,float y) {
        for(int i=0;i<600;i++){var v=w.snapshot();float dx=x-v.x(),dy=y-v.y();if(Math.hypot(dx,dy)<8)return;w.attack();w.tick(dx,dy);}
        fail("Nao chegou ao ponto "+x+","+y);
    }
    @Test void createQuestFightLootTradeEquipSaveAndResume() throws Exception {
        var w=new RpgWorld(CharacterIdentity.create("Aurora Teste","reaper","azure"));assertTrue(w.interact());assertTrue(w.acceptQuest("first_steps"));
        move(w,855,320);assertTrue(w.interact());assertEquals("forest",w.save().region());
        for(int i=0;i<800&&w.save().quests().get("first_steps")<2;i++){w.attack();w.tick(0,0);}
        assertEquals(2,w.save().quests().get("first_steps"));assertTrue(w.inventory().count("crystal")>=2);assertTrue(w.save().hp()>0);
        move(w,80,320);assertTrue(w.interact());move(w,190,320);assertTrue(w.interact());assertTrue(w.completeQuest("first_steps"));assertTrue(w.level()>=2);
        move(w,230,410);assertTrue(w.interact());assertTrue(w.trade("sword",1,true));assertTrue(w.equip("sword"));
        var store=new LocalSaveStore(folder.resolve("offline.properties"));store.save(w.save());var restored=RpgWorld.restore(store.load());assertEquals(w.save(),restored.save());assertEquals(w.attackPower(),restored.attackPower());
    }
}
