package com.projetok.rpg;

import com.projetok.domain.entity.component.StatsComponent;
import java.util.*;

/** Cópia observacional interna; não é DTO recuperável nem autorização de ativação. */
public record CharacterReadState(CharacterIdentity identity, String region, float x, float y, long tick,
        long xp, DomainProgression domain, StatsComponent rawAttributes, boolean manualAttributesEnabled,
        int hp, int energy, double regenerationRemainder, int maximumEnergy,
        int capacity, List<InventoryService.Stack> slots, Map<String,String> equipment, int coins,
        long attackReadyTick, double preciseAttackReady, long shunpoReadyTick,
        long potionReadyTick, long travelReadyTick, Restrictions restrictions,
        QuestService.Ledger builtinQuests, QuestService.Ledger authoredQuests, QuestBook authoredBook) {
    /** Preserva índices vazios reais da bolsa e copia todas as coleções mutáveis. */
    public CharacterReadState {
        slots = Collections.unmodifiableList(new ArrayList<>(slots));
        equipment = Map.copyOf(equipment);
    }

    /**
     * Timers locais e flags observáveis, sem origem inventada nem execução exportada.
     * Efeitos externos/DoTs e sua política de recuperação permanecem desconhecidos.
     */
    public record Restrictions(long rootUntil, long stunUntil, long slowUntil, float slowScale,
            long evadeUntil, boolean transitioning, boolean dashing, boolean sentry,
            boolean intangible, boolean externalEffectsKnown) { }
}
