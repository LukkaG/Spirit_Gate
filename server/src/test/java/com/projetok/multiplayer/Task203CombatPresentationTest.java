package com.projetok.multiplayer;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task203CombatPresentationTest {
    private Protocol.State state(int tick,int hp,int flags) {
        return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Jogador",140,300,hp,100,1000,0,false,1,0,flags)));
    }
    @Test void authoritativeAttackHitDeathAndRecoveryAreIdempotent() {
        var view=new NetworkCombatPresentation();
        view.update(state(10,100,1),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.START,view.phase(1));
        view.update(state(12,100,1),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.STRIKE,view.phase(1));
        view.update(state(16,100,1),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.RECOVER,view.phase(1));
        view.update(state(22,100,0),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.IDLE,view.phase(1));
        view.update(state(23,80,0),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.HIT,view.phase(1));
        view.update(state(23,100,1),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.HIT,view.phase(1));
        view.update(state(30,0,0),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.DEAD,view.phase(1));
        view.update(state(31,100,0),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.IDLE,view.phase(1));
        view.update(new Protocol.State(32,List.of()),List.of(),1);assertEquals(NetworkCombatPresentation.Phase.IDLE,view.phase(1));
    }
    @Test void acceptedServerAttackIsVisibleForBothPlayersWithoutApplyingDamageTwice() {
        var world=new RpgSessionWorld(List.of("spirit","human"),null,true);
        assertTrue(world.join(1,"Esp Agora"));assertTrue(world.join(2,"Human Agora"));
        assertTrue(world.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));
        int hp=world.combatSnapshot().session().world().actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow().hp();
        assertFalse(world.accept(1,new Protocol.Intent(1,Protocol.Action.ATTACK,0,0)));
        for(int self:List.of(1,2)) {
            var view=new NetworkCombatPresentation();var state=world.combatSnapshotFor(self);
            view.update(state.session().world(),state.events(),self);
            assertEquals(NetworkCombatPresentation.Phase.START,view.phase(1));
        }
        assertEquals(hp,world.combatSnapshot().session().world().actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow().hp());
    }
}
