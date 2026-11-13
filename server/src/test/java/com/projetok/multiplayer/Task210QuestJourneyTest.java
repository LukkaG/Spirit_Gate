package com.projetok.multiplayer;

import com.projetok.studio.StudioExport;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita objetivo, entrega e replay de missão com dois personagens e combate TCP reais. */
@Timeout(25)
class Task210QuestJourneyTest {
    /** ACTIVE só vira READY depois da morte no ledger, sem manipular o estado da missão. */
    @Test void realTwoClientSessionCompletesIndividualQuestAndCannotReplayReward()throws Exception {
        try(var server=Task225R1CombatFixture.start();
            var a=Task207InventoryTest.connect(server,"Missao Spirit","spirit");var b=Task207InventoryTest.connect(server,"Missao Human","human")) {
            Task201CharacterSessionTest.ready(a,b);
            travel(a,b,"forest","to_village","village");
            a.request(Protocol.WorldAction.INTERACT,"elder");a.request(Protocol.WorldAction.ACCEPT_QUEST,"coop_slime");
            Task201CharacterSessionTest.waitFor(()->a.quests().get(0).state().equals("ACTIVE"),a,b);
            assertEquals(0,a.quests().get(0).progress());
            assertEquals("AVAILABLE",b.quests().get(0).state());
            var otherInventory=b.playerSheet().inventory();
            int otherCoins=coins(b);
            long until=a.snapshot().tick()+31;a.await(s->s.tick()>until,Duration.ofSeconds(3));
            travel(a,b,"village","to_forest","forest");
            var death=Task225R1CombatFixture.defeatSlime(server,a,b);
            assertEquals("READY",a.quests().get(0).state());
            assertEquals(1,a.quests().get(0).progress());assertEquals(1,a.quests().get(0).required());
            assertEquals(3,a.playerSheet().progress().xp());assertEquals(0,b.playerSheet().progress().xp());
            int potions=Task207InventoryTest.count(a.playerSheet(),"potion");
            a.request(Protocol.WorldAction.COMPLETE_QUEST,"coop_slime");
            long refusedAt=a.snapshot().tick();a.await(s->s.tick()>refusedAt+3,Duration.ofSeconds(3));
            assertEquals("READY",a.quests().get(0).state(),"Entrega longe da Anciã não deve concluir");
            assertEquals(potions,Task207InventoryTest.count(a.playerSheet(),"potion"));
            assertEquals(1,Task225R1CombatFixture.events(b,"DEATH",death).size());
            assertEquals(1,Task225R1CombatFixture.events(b,"SYNTHETIC_REWARD",death).size());
            travel(a,b,"forest","to_village","village");
            a.request(Protocol.WorldAction.INTERACT,"elder");a.request(Protocol.WorldAction.COMPLETE_QUEST,"coop_slime");
            Task201CharacterSessionTest.waitFor(()->a.quests().get(0).state().equals("COMPLETED"),a,b);
            assertEquals(potions+1,Task207InventoryTest.count(a.playerSheet(),"potion"));
            var completed=a.playerSheet();
            int completedCoins=coins(a);
            a.request(Protocol.WorldAction.COMPLETE_QUEST,"coop_slime");long finalTick=a.snapshot().tick()+3;
            a.await(s->s.tick()>finalTick,Duration.ofSeconds(3));assertEquals(potions+1,Task207InventoryTest.count(a.playerSheet(),"potion"));
            assertEquals(completed,a.playerSheet(),"Replay não concede XP, moedas ou inventário adicional");
            assertEquals(completedCoins,coins(a),"Replay não concede moedas adicionais");
            assertEquals("AVAILABLE",b.quests().get(0).state());assertEquals(0,b.quests().get(0).progress());
            assertEquals(otherInventory,b.playerSheet().inventory());assertEquals(0,b.playerSheet().progress().xp());
            assertEquals(otherCoins,coins(b),"Recompensa não é transferida ao observador");
            assertNull(server.failure());
        }
    }
    /** As moedas pertencem ao ator do snapshot; não fazem parte da ficha de inventário. */
    private static int coins(LocalClient client){
        return client.snapshot().actors().stream().filter(actor->actor.id()==client.id()).findFirst().orElseThrow().coins();
    }
    /** Viaja pelo mesmo comando de portal validado no servidor; não reposiciona personagens pelo teste. */
    static void travel(LocalClient a,LocalClient b,String from,String id,String destination)throws Exception {
        a.request(Protocol.WorldAction.TRAVEL,StudioExport.key("portal_",from+"/"+id));
        Task201CharacterSessionTest.waitFor(()->a.region().equals(destination),a,b);
    }
}
