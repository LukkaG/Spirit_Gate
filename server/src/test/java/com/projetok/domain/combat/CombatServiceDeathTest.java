package com.projetok.domain.combat;

import com.projetok.domain.entity.*;
import com.projetok.domain.entity.capability.*;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityDeathEvent;
import com.projetok.domain.race.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatServiceDeathTest {
    static class Target extends GameEntity implements Damageable {
        int hp=1;
        Target() { super(2,"Alvo",new Position(100,100)); }
        public void receiveDamage(int damage) { hp=Math.max(0,hp-damage); }
        public boolean isDead() { return hp==0; }
        public Race race() { return RaceRegistry.withDefaults().get("human"); }
    }
    @Test void deathIsPublishedOnlyOnTheLiveToDeadTransition() {
        var bus=new GameEventBus();int[] deaths={0};int[] calculations={0};
        bus.subscribe(EntityDeathEvent.class,e->deaths[0]++);
        var combat=new CombatService(bus);var target=new Target();
        Attacker attacker=new Attacker() {
            public int computeOutgoingDamage(){calculations[0]++;return 10;}
            public DamageType outgoingDamageType(){return DamageType.PHYSICAL;}
        };
        assertEquals(10,combat.attack(attacker,target));
        assertEquals(0,combat.attack(attacker,target));
        assertEquals(1,deaths[0]);assertEquals(1,calculations[0]);
    }
}
