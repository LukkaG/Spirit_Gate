package com.projetok.domain.effect;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import java.util.Objects;

/** Aplica e atualiza efeitos no domínio, exclusivamente na thread da simulação. */
public final class StatusEffectService {
    /** Cria um serviço sem estado compartilhado entre entidades ou habilidades. */
    public StatusEffectService() { }

    // --- Elegibilidade e aplicação ---

    /**
     * Renova um efeito por uma duração finita e positiva, sem acumular tempo.
     * Recusa mortos e, neste ciclo, permite congelamento somente em mobs.
     *
     * @return verdadeiro quando o efeito foi aplicado; falso para alvo inelegível
     */
    public boolean apply(GameEntity entity, StatusEffectType type, float duration) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(type, "type");
        if (!Float.isFinite(duration) || duration <= 0f)
            throw new IllegalArgumentException("Duração do efeito deve ser finita e positiva");
        if (type == StatusEffectType.FREEZE && !(entity instanceof Mob)) return false;
        if (entity.require(HealthComponent.class).isDead()) return false;
        StatusEffectsComponent effects = entity.get(StatusEffectsComponent.class).orElseGet(() -> {
            var created = new StatusEffectsComponent();
            entity.attach(created);
            return created;
        });
        effects.refresh(type, duration);
        return true;
    }

    // --- Relógio autoritativo ---

    /**
     * Avança efeitos anteriores ao próximo pulso ou comando; mortos perdem seus efeitos.
     * O coordenador chama este método antes de aplicar novos efeitos no mesmo tick.
     */
    public void update(GameEntity entity, float dt) {
        Objects.requireNonNull(entity, "entity");
        if (!Float.isFinite(dt) || dt < 0f)
            throw new IllegalArgumentException("Delta do efeito deve ser finito e não negativo");
        entity.get(StatusEffectsComponent.class).ifPresent(effects -> {
            if (entity.require(HealthComponent.class).isDead()) effects.clear();
            else effects.advance(dt);
        });
    }
}
