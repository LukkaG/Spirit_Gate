package com.projetok.multiplayer;

import com.projetok.studio.LiveRevision;
import com.projetok.studio.WorldContent;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Sequências e cancelamento de revisão exercitados no coordenador real e no mesmo owner do teste. */
class Alpha002LightningSessionTest {
    private record Fixture(RpgSessionWorld world,LiveRevision revision){ }

    /** Usa admissão real da aplicação 7; não altera campos privados nem inventa endpoint de teste. */
    private static Fixture fixture()throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","reaper"),null,true,20);
        var revision=LiveRevision.prepare(0,"forest",WorldContent.baseline().bundle().maps(),Task201CharacterSessionTest.EMPTY);
        world.enableRevisions(revision);
        assertTrue(world.join(1,new SessionCharacterSelection("Raio","reaper","none","none"),7));
        boolean resumed=false;
        for(int i=0;i<5000&&!resumed;i++){
            var message=world.nextContent(1);assertNotNull(message,"Bootstrap deve avançar até RESUME");
            if(message.kind()==Protocol.ContentKind.END)
                world.contentAck(1,new Protocol.Content(Protocol.ContentKind.READY,message.transaction(),message.hash(),0,new byte[0]));
            if(message.kind()==Protocol.ContentKind.ACTIVATE)
                world.contentAck(1,new Protocol.Content(Protocol.ContentKind.ACTIVE,message.transaction(),message.hash(),0,new byte[0]));
            resumed=message.kind()==Protocol.ContentKind.RESUME;
        }
        assertTrue(resumed);
        assertTrue(world.gameplay(1,new GameplayProtocol.Request(1,GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder")));
        return new Fixture(world,revision);
    }

    @Test void refusedActionsConsumeSequenceAndNeverQueueMovementOrResetCooldown()throws Exception {
        var f=fixture();var world=f.world();var origin=world.snapshot().actors().get(0);
        var cast=new SkillProtocol.Request(2,5,0,0,0,0);assertTrue(world.skill(1,cast));
        assertFalse(world.skill(1,cast));
        assertFalse(world.accept(1,new Protocol.Intent(3,Protocol.Action.ATTACK,0,0)));
        assertFalse(world.request(1,new Protocol.WorldRequest(4,Protocol.WorldAction.TRAVEL,"to_village")));
        var move=new Protocol.Intent(5,Protocol.Action.MOVE,1,0);assertFalse(world.accept(1,move));
        for(int i=0;i<33;i++)world.step();
        assertFalse(world.accept(1,move));assertFalse(world.skill(1,cast));
        assertFalse(world.skill(1,new SkillProtocol.Request(6,5,0,0,0,0)));
        assertEquals(237,world.skillsSnapshotFor(1).cooldowns().get(4));
        assertEquals(origin.x(),world.snapshot().actors().get(0).x());
        assertEquals(origin.y(),world.snapshot().actors().get(0).y());
        assertEquals("forest",world.region(1));
        assertTrue(world.accept(1,new Protocol.Intent(7,Protocol.Action.MOVE,0,1)));
        world.step();assertNotEquals(origin.y(),world.snapshot().actors().get(0).y());
    }

    @Test void revisionReplacementCancelsExecutionWithoutTransferringPhaseOrLateImpacts()throws Exception {
        var f=fixture();var world=f.world();assertTrue(world.skill(1,new SkillProtocol.Request(2,5,0,0,0,0)));
        world.step();assertEquals(1,world.skillsSnapshotFor(1).events().size());
        var replacement=world.prepareRevision(f.revision());world.installRevision(replacement);
        for(int i=0;i<40;i++)world.step();
        assertTrue(world.skillsSnapshotFor(1).events().isEmpty());
        assertTrue(world.combatSnapshotFor(1).events().stream().noneMatch(e->e.player()==1&&e.kind().equals("HIT")));
        assertTrue(world.accept(1,new Protocol.Intent(3,Protocol.Action.MOVE,0,1)));
    }
}
