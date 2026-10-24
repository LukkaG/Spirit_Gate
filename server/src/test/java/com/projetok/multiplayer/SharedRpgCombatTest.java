package com.projetok.multiplayer;

import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedRpgCombatTest {
    private RpgSessionWorld world(){return new RpgSessionWorld(List.of("spirit","reaper"),null,true);}
    @Test void sharedDamageDeathRewardsRespawnAndReplayProtection() throws Exception {
        var w=world();assertTrue(w.join(1,"Jogador A"));assertTrue(w.join(2,"Jogador B"));
        assertEquals(3,w.combatSnapshot().session().world().actors().size());
        assertTrue(w.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));
        assertFalse(w.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));
        assertFalse(w.accept(1,new Protocol.Intent(2,Protocol.Action.ATTACK,0,0)));
        // Segundo personagem aproxima-se sem conceder coordenadas finais ao cliente.
        assertTrue(w.accept(2,new Protocol.Intent(1,Protocol.Action.MOVE,0,-1)));
        for(int i=0;i<9;i++)w.step();
        assertTrue(w.accept(2,new Protocol.Intent(2,Protocol.Action.MOVE,0,0)));
        for(int i=0;i<120&&enemy(w).hp()>0;i++){
            w.step();if(i%12==0){w.accept(1,new Protocol.Intent(10+i,Protocol.Action.ATTACK,0,0));w.accept(2,new Protocol.Intent(10+i,Protocol.Action.ATTACK,0,0));}
        }
        assertEquals(0,enemy(w).hp());
        var events=w.combatSnapshot().events();assertEquals(1,events.stream().filter(e->e.kind().equals("DEATH")).count());
        assertEquals(2,events.stream().filter(e->e.kind().equals("SYNTHETIC_REWARD")).count());
        assertFalse(w.accept(2,new Protocol.Intent(999,Protocol.Action.ATTACK,0,0)));
        for(int i=0;i<241;i++)w.step();assertTrue(enemy(w).hp()>0);
        w.leave(1);w.leave(2);assertEquals(0,w.snapshot().actors().size());
        assertEquals(40,enemy(world()).hp());
    }
    private Protocol.Actor enemy(RpgSessionWorld w){return w.combatSnapshot().session().world().actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow();}
    @Test void combatFrameRoundTripAndLegacyBytesRemainStable()throws Exception{
        var w=world();w.join(1,"Jogador A");w.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0));var snapshot=w.combatSnapshot();
        var bytes=new ByteArrayOutputStream();Protocol.write(bytes,snapshot);assertEquals(snapshot,Protocol.read(new ByteArrayInputStream(bytes.toByteArray())));assertTrue(bytes.size()<Protocol.MAX_FRAME);
        bytes.reset();Protocol.write(bytes,new Protocol.Bye());assertArrayEquals(new byte[]{0,0,0,2,1,5},bytes.toByteArray());
    }
    @Test void realTcpPeersShareHpAndNegotiateSeparately()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world())){
            server.start();try(var a=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Jogador A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Jogador B",false)){
                a.await(s->s.actors().size()==3,Duration.ofSeconds(3));b.await(s->s.actors().size()==3,Duration.ofSeconds(3));
                a.send(Protocol.Action.ATTACK,0,0);
                var first=a.await(s->s.actors().stream().anyMatch(v->v.boss()&&v.hp()<40),Duration.ofSeconds(3));
                int hp=first.actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow().hp();
                b.await(s->s.actors().stream().anyMatch(v->v.boss()&&v.hp()==hp),Duration.ofSeconds(3));assertNull(server.failure());
                assertTrue(a.measureLatency());a.await(s->Double.isFinite(a.latencyMillis()),Duration.ofSeconds(3));assertTrue(a.latencyMillis()>=0);
            }
            try(var legacy=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Legado A")){legacy.await(s->!s.actors().isEmpty(),Duration.ofSeconds(3));assertTrue(legacy.snapshot().actors().stream().noneMatch(Protocol.Actor::boss));}
        }
    }
    @Test void invalidActionsCannotChangeEnemyState(){var w=world();w.join(1,"Jogador A");assertFalse(w.accept(2,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));assertFalse(w.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,999,0)));assertFalse(w.accept(1,new Protocol.Intent(2,Protocol.Action.LIGHTNING_STEP,0,0)));assertFalse(w.accept(1,new Protocol.Intent(3,Protocol.Action.CRYOSTASIS,0,0)));assertEquals(40,enemy(w).hp());}
    @Test void portalVisibilityQuestAndReturnAreAuthoritative(){
        var w=world();w.join(1,"Jogador A");w.join(2,"Jogador B");
        assertFalse(w.request(1,new Protocol.WorldRequest(1,Protocol.WorldAction.TRAVEL,"missing")));
        assertTrue(w.request(1,new Protocol.WorldRequest(2,Protocol.WorldAction.TRAVEL,"to_village")));
        assertEquals("village",w.region(1));assertEquals("forest",w.region(2));
        assertEquals(1,w.snapshotFor(1).actors().size());assertEquals(1,w.snapshotFor(2).actors().size());
        assertTrue(w.combatSnapshotFor(1).session().world().actors().stream().noneMatch(a->a.id()==1_000_100||a.id()==2));
        assertTrue(w.request(1,new Protocol.WorldRequest(3,Protocol.WorldAction.INTERACT,"elder")));
        assertTrue(w.request(1,new Protocol.WorldRequest(4,Protocol.WorldAction.ACCEPT_QUEST,"coop_slime")));
        assertFalse(w.request(1,new Protocol.WorldRequest(5,Protocol.WorldAction.ACCEPT_QUEST,"coop_slime")));
        assertFalse(w.request(1,new Protocol.WorldRequest(6,Protocol.WorldAction.COMPLETE_QUEST,"coop_slime")));
        for(int i=0;i<31;i++)w.step();
        assertTrue(w.request(1,new Protocol.WorldRequest(7,Protocol.WorldAction.TRAVEL,"to_forest")));
        for(int i=0;i<150&&enemy(w).hp()>0;i++){w.step();if(i%12==0)w.accept(1,new Protocol.Intent(20+i,Protocol.Action.ATTACK,0,0));}
        assertEquals(0,enemy(w).hp());assertEquals("READY",w.combatSnapshotFor(1).quests().get(0).state());
        // Retorna ao NPC para concluir; não existe conclusão remota arbitrária.
        for(int i=0;i<31;i++)w.step();
        assertTrue(w.request(1,new Protocol.WorldRequest(200,Protocol.WorldAction.TRAVEL,"to_village")));
        assertTrue(w.request(1,new Protocol.WorldRequest(201,Protocol.WorldAction.INTERACT,"elder")));
        assertTrue(w.request(1,new Protocol.WorldRequest(202,Protocol.WorldAction.COMPLETE_QUEST,"coop_slime")));
        assertFalse(w.request(1,new Protocol.WorldRequest(203,Protocol.WorldAction.COMPLETE_QUEST,"coop_slime")));
        assertEquals("COMPLETED",w.combatSnapshotFor(1).quests().get(0).state());
    }
    @Test void syntheticLevelTwentyReusesShunpoAndSharedCooldown(){
        var w=new RpgSessionWorld(List.of("reaper","human"),null,true,20);w.join(1,"Jogador A");w.join(2,"Jogador B");
        assertTrue(w.accept(1,new Protocol.Intent(1,Protocol.Action.LIGHTNING_STEP,0,0)));
        assertFalse(w.accept(1,new Protocol.Intent(2,Protocol.Action.CRYOSTASIS,0,0)));
        assertFalse(w.accept(1,new Protocol.Intent(1,Protocol.Action.LIGHTNING_STEP,0,0)));
        assertEquals(750,w.snapshot().actors().get(0).stamina());
        assertTrue(w.combatSnapshotFor(2).session().world().actors().stream().anyMatch(a->a.id()==1&&a.x()>140));
    }
    @Test void identitiesAreEphemeralAndDelayedJoinDoesNotAdvanceEnemyRespawn(){
        var w=world();w.join(1,"Jogador A");var identity=w.sessionIdentity(1);
        var restarted=world();restarted.join(1,"Jogador A");assertNotEquals(identity,restarted.sessionIdentity(1));
        for(int i=0;i<300;i++)w.step();w.leave(1);assertEquals(0,w.snapshotFor(1).actors().size());
        assertTrue(w.join(2,"Retorno A"));assertNotEquals(identity,w.sessionIdentity(2));
        // O relógio global da população não é reiniciado pelo relógio local do personagem.
        for(int i=0;i<120&&enemy(w).hp()>0;i++){w.step();if(i%12==0)w.accept(2,new Protocol.Intent(i+1,Protocol.Action.ATTACK,0,0));}
        assertEquals(0,enemy(w).hp());for(int i=0;i<100;i++)w.step();assertEquals(0,enemy(w).hp());
    }
    @Test void deadPlayerCannotAttackAndCanRespawnOnceAtSafeHome(){var w=world();w.join(1,"Jogador A");for(int i=0;i<1200;i++)w.step();assertEquals(0,w.snapshot().actors().get(0).hp());assertFalse(w.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));assertTrue(w.request(1,new Protocol.WorldRequest(2,Protocol.WorldAction.RESPAWN,"home")));assertTrue(w.snapshot().actors().get(0).hp()>0);assertFalse(w.request(1,new Protocol.WorldRequest(3,Protocol.WorldAction.RESPAWN,"home")));}
    @Test void rewardPolicyCanRefuseAllSyntheticRewards(){var w=new RpgSessionWorld(List.of("spirit","human"),null,true,1,com.projetok.rpg.SharedRpgEncounter.RewardPolicy.DISABLED);w.join(1,"Jogador A");for(int i=0;i<100&&enemy(w).hp()>0;i++){w.step();if(i%12==0)w.accept(1,new Protocol.Intent(i+1,Protocol.Action.ATTACK,0,0));}assertEquals(0,enemy(w).hp());assertEquals(1,w.combatSnapshot().events().stream().filter(e->e.kind().equals("REWARD_REFUSED")).count());assertEquals(0,w.snapshot().actors().get(0).coins());}
}
