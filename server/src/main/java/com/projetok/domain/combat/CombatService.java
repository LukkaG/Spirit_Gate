package com.projetok.domain.combat;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.capability.Attacker;
import com.projetok.domain.entity.capability.Damageable;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityDeathEvent;

import java.util.Objects;

/**
 * Único caminho de dano do jogo. Substitui Entity.currentDefense() + CombatCalculator +
 * DefenseCalculator (que existiam soltos e desconectados no código original — nenhum dos
 * três realmente aplicava dano a um alvo) por um só lugar. Player ataca Mob, Mob ataca
 * Player, e futuramente Mob pode atacar Mob — todos passam por aqui, porque Player e Mob
 * implementam as mesmas duas interfaces (Attacker, Damageable).
 */
public final class CombatService {

    private final GameEventBus eventBus;

    public CombatService(GameEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    public <T extends GameEntity & Damageable> int attack(Attacker attacker, T target) {
        int rawDamage = attacker.computeOutgoingDamage();
        double affinity = target.race().affinityFor(attacker.outgoingDamageType());
        int finalDamage = (int) Math.round(rawDamage * affinity);

        target.receiveDamage(Math.max(0, finalDamage));

        if (target.isDead()) {
            eventBus.publish(new EntityDeathEvent(target));
        }

        return finalDamage;
    }
}
